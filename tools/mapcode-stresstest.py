#!/usr/bin/env python3
"""Stresstest the mapcode API at a target average request rate.

Fires encode and decode requests at a long-run average of --rate req/s using
a compound-Poisson-style schedule: each iteration draws a random burst size
(exponential, mean grows with the target rate) and a jittered inter-burst
delay so the traffic is bursty rather than evenly spaced — e.g. at 2 req/s
you may see 4 requests in parallel and then no traffic for 2 seconds.

A curses TUI shows two scrolling panes plus a header dashboard:
  - top pane:    every request with its HTTP status code
  - bottom pane: only failed requests with the error detail

Press Esc to pause; any key resumes. Ctrl-C aborts.
"""
from __future__ import annotations

import argparse
import curses
import http.client
import json
import locale
import math
import queue
import random
import socket
import sys
import threading
import time
import urllib.parse
from collections import deque
from concurrent.futures import CancelledError, ThreadPoolExecutor

REQUEST_TIMEOUT = 10.0
USER_AGENT = "mapcode-stresstest/1.0"

# Fallback decoded targets used only until the live bucket has been seeded by
# encode responses. A few of these may be invalid against any given service;
# that is intentional and acceptable per the script's design.
SAMPLE_MAPCODES: list[tuple[str, str | None]] = [
    ("49.4V", "NLD"),
    ("PQ.NR", "NLD"),
    ("R8.5C", "NLD"),
    ("VHXGB.1J9J", None),
    ("XXVVV.74JV", None),
    ("HSGV.HHC", "USA"),
    ("PB000.00", "NLD"),
]

SPINNER = "⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏"


def random_encode_url(base: str) -> str:
    lat = random.uniform(-85.0, 85.0)
    lon = random.uniform(-180.0, 180.0)
    return f"{base}/mapcode/codes/{lat:.6f},{lon:.6f}"


def decode_url(base: str, mapcode: str, context: str | None) -> str:
    url = f"{base}/mapcode/coords/{urllib.parse.quote(mapcode)}"
    if context:
        url += f"?context={urllib.parse.quote(context)}"
    return url


# Per-worker-thread HTTP connection cache so the stress test measures server
# behavior rather than fresh-connection overhead. Each thread keeps one open
# connection per (scheme, host, port); stale connections are dropped on first
# error and reopened on retry.
_tls = threading.local()


def _get_conn(scheme: str, host: str, port: int) -> http.client.HTTPConnection:
    conns = getattr(_tls, "conns", None)
    if conns is None:
        conns = {}
        _tls.conns = conns
    key = (scheme, host, port)
    c = conns.get(key)
    if c is None:
        if scheme == "https":
            c = http.client.HTTPSConnection(host, port, timeout=REQUEST_TIMEOUT)
        else:
            c = http.client.HTTPConnection(host, port, timeout=REQUEST_TIMEOUT)
        conns[key] = c
    return c


def _drop_conn(scheme: str, host: str, port: int) -> None:
    conns = getattr(_tls, "conns", None)
    if not conns:
        return
    key = (scheme, host, port)
    c = conns.pop(key, None)
    if c is not None:
        try:
            c.close()
        except Exception:  # noqa: BLE001 - best-effort cleanup
            pass


_STALE_CONN_ERRORS = (
    http.client.RemoteDisconnected,
    http.client.BadStatusLine,
    BrokenPipeError,
    ConnectionResetError,
    ConnectionAbortedError,
)


def do_request(url: str) -> tuple[bool, int | None, str | None, bytes | None, int]:
    """Run one HTTP request. Returns (success, code, msg, body, duration_ms).

    Reuses a per-thread keep-alive connection. On stale-connection errors the
    cached connection is dropped and the request is retried once.
    """
    parsed = urllib.parse.urlsplit(url)
    scheme = parsed.scheme or "http"
    host = parsed.hostname or ""
    default_port = 443 if scheme == "https" else 80
    port = parsed.port or default_port
    path = parsed.path or "/"
    if parsed.query:
        path += "?" + parsed.query
    headers = {"Accept": "application/json", "User-Agent": USER_AGENT}

    t0 = time.monotonic()
    last_err: Exception | None = None
    for attempt in (1, 2):
        try:
            c = _get_conn(scheme, host, port)
            c.request("GET", path, headers=headers)
            r = c.getresponse()
            data = r.read()
            if 200 <= r.status < 400:
                return True, r.status, None, data, _ms_since(t0)
            return False, r.status, f"HTTP {r.status} {r.reason}", None, _ms_since(t0)
        except _STALE_CONN_ERRORS as e:
            _drop_conn(scheme, host, port)
            last_err = e
            if attempt == 1:
                continue
            return False, None, f"{type(e).__name__}: {e}", None, _ms_since(t0)
        except (socket.timeout, TimeoutError):
            # Timeouts shouldn't retry — that would compound the load on an
            # already-struggling server. Surface the configured threshold so
            # the error pane shows what was actually exceeded.
            _drop_conn(scheme, host, port)
            return (
                False,
                None,
                f"Timeout after {int(REQUEST_TIMEOUT * 1000)} ms",
                None,
                _ms_since(t0),
            )
        except Exception as e:  # noqa: BLE001 - the UI labels what we hit
            _drop_conn(scheme, host, port)
            return False, None, f"{type(e).__name__}: {e}", None, _ms_since(t0)
    # Both attempts hit stale-conn errors.
    return False, None, f"{type(last_err).__name__}: {last_err}", None, _ms_since(t0)


def _ms_since(t0: float) -> int:
    return int((time.monotonic() - t0) * 1000)


def extract_mapcodes(payload: object) -> list[tuple[str, str | None]]:
    """Walk a parsed JSON response and pull (mapcode, territory) pairs.

    Matches any nested object that has a 'mapcode' string field. Territory is
    captured when present; when absent the mapcode is treated as international
    and decoded without a context. The encode response carries the same
    mapcode in multiple places (local, international, mapcodes[]) — duplicates
    are fine, the bucket is a rolling FIFO.
    """
    out: list[tuple[str, str | None]] = []

    def walk(node: object) -> None:
        if isinstance(node, dict):
            mc = node.get("mapcode")
            if isinstance(mc, str) and mc:
                terr = node.get("territory")
                out.append((mc, terr if isinstance(terr, str) and terr else None))
            for v in node.values():
                walk(v)
        elif isinstance(node, list):
            for v in node:
                walk(v)

    walk(payload)
    return out


class UI:
    def __init__(self, stdscr: "curses._CursesWindow", args: argparse.Namespace) -> None:
        self.stdscr = stdscr
        self.args = args
        self.base = args.url.rstrip("/")

        curses.curs_set(0)
        stdscr.nodelay(True)
        stdscr.keypad(True)
        try:
            curses.set_escdelay(25)
        except (AttributeError, curses.error):
            pass

        self._init_colors()

        self.events: queue.Queue = queue.Queue()
        self.lock = threading.Lock()
        self.ok = 0
        self.err = 0
        self.cancelled = 0
        self.total_fired = 0
        self.fired_this_window = 0
        self.last_rate = 0.0
        self.started = time.monotonic()
        self.window_start = self.started
        self.spinner_i = 0
        self.paused = False
        self.aborted = False

        # Rolling FIFO of (mapcode, territory) pairs harvested from successful
        # encode responses; decode requests sample from it. Falls back to the
        # hardcoded SAMPLE_MAPCODES while the bucket is still empty.
        self._bucket: deque[tuple[str, str | None]] = deque(maxlen=args.bucket_size)
        self._bucket_lock = threading.Lock()

        # Smoothed burst-size display: average of the last few bursts so the
        # header doesn't flicker between e.g. "burst 17" and "burst 1".
        self._recent_bursts: deque[int] = deque(maxlen=16)

        # Server version polled in the background; rendered in the header.
        self._server_version: str | None = None
        self._version_lock = threading.Lock()
        self._version_stop = threading.Event()

        self._layout_too_small = False
        if not self._make_layout():
            raise RuntimeError(
                f"Terminal too small (need >= 60x14, got {self.w}x{self.h})."
            )

    def _init_colors(self) -> None:
        self.colors = curses.has_colors()
        if self.colors:
            curses.start_color()
            try:
                curses.use_default_colors()
                bg = -1
            except curses.error:
                bg = curses.COLOR_BLACK
            curses.init_pair(1, curses.COLOR_GREEN, bg)
            curses.init_pair(2, curses.COLOR_RED, bg)
            curses.init_pair(3, curses.COLOR_YELLOW, bg)
            curses.init_pair(4, curses.COLOR_CYAN, bg)
            curses.init_pair(5, curses.COLOR_MAGENTA, bg)
        self.A_OK = curses.color_pair(1) if self.colors else 0
        self.A_ERR = curses.color_pair(2) if self.colors else 0
        self.A_WARN = curses.color_pair(3) if self.colors else 0
        self.A_INFO = curses.color_pair(4) if self.colors else 0
        self.A_ACCENT = curses.color_pair(5) if self.colors else 0
        self.A_DIM = curses.A_DIM
        self.A_BOLD = curses.A_BOLD

    def _make_layout(self) -> bool:
        """Build (or rebuild) all windows. Returns False if the terminal is
        too small to draw — callers should display a placeholder instead."""
        self.h, self.w = self.stdscr.getmaxyx()
        if self.h < 14 or self.w < 60:
            self._layout_too_small = True
            try:
                self.stdscr.erase()
                self.stdscr.refresh()
            except curses.error:
                pass
            return False
        self._layout_too_small = False

        header_h = 4
        footer_h = 1
        remain = self.h - header_h - footer_h
        req_h = remain // 2 + (remain % 2)
        err_h = remain - req_h

        self.hdr = curses.newwin(header_h, self.w, 0, 0)
        self.req_box = curses.newwin(req_h, self.w, header_h, 0)
        self.err_box = curses.newwin(err_h, self.w, header_h + req_h, 0)
        self.foot = curses.newwin(footer_h, self.w, self.h - footer_h, 0)

        # Independent inner windows (not derwin) so they keep their own
        # backing buffer; otherwise erasing the outer box every frame would
        # wipe the scrolled history we just wrote.
        self.req_inner = curses.newwin(req_h - 2, self.w - 2, header_h + 1, 1)
        self.err_inner = curses.newwin(
            err_h - 2, self.w - 2, header_h + req_h + 1, 1
        )
        for win in (self.req_inner, self.err_inner):
            win.scrollok(True)
            win.idlok(True)

        self.stdscr.erase()
        self.stdscr.noutrefresh()

        # Borders are static: draw once here, then leave alone each frame.
        self._draw_frame(self.req_box, "REQUESTS", self.A_INFO)
        self._draw_frame(self.err_box, "ERRORS", self.A_ERR)
        self.req_box.noutrefresh()
        self.err_box.noutrefresh()
        self.req_inner.noutrefresh()
        self.err_inner.noutrefresh()
        return True

    @staticmethod
    def _fmt_duration(secs: float) -> str:
        m, s = divmod(int(secs), 60)
        h, m = divmod(m, 60)
        if h:
            return f"{h}h{m:02d}m{s:02d}s"
        if m:
            return f"{m}m{s:02d}s"
        return f"{secs:.1f}s"

    def _status_color(self, code: int | None) -> int:
        if code is None:
            return self.A_ERR
        if 200 <= code < 300:
            return self.A_OK
        if 300 <= code < 400:
            return self.A_INFO
        if 400 <= code < 500:
            return self.A_WARN
        return self.A_ERR

    def _short(self, url: str) -> str:
        return url[len(self.base):] if url.startswith(self.base) else url

    def _safe_addstr(self, win, y: int, x: int, text: str, attr: int = 0) -> None:
        try:
            max_w = win.getmaxyx()[1]
            if x >= max_w - 1:
                return
            win.addnstr(y, x, text, max_w - 1 - x, attr)
        except curses.error:
            pass

    def _add_line(self, win, segments: list[tuple[str, int]]) -> None:
        """Append one logical entry, wrapping across rows if too wide.

        Each segment is a (text, attr) pair. We lay out the segments left to
        right; whenever we'd run off the right edge we wrap to a new row with
        a small continuation indent so multi-row entries stay visually grouped.
        """
        h, w = win.getmaxyx()
        width = w - 1  # leave the rightmost column blank (avoids edge-scroll)
        if width <= 0 or h <= 0:
            return

        continuation_indent = 2 if width > 8 else 0

        rows: list[list[tuple[str, int]]] = [[]]
        used = 0
        for text, attr in segments:
            if not text:
                continue
            i = 0
            while i < len(text):
                if used >= width:
                    rows.append([])
                    used = 0
                    if continuation_indent:
                        rows[-1].append((" " * continuation_indent, 0))
                        used = continuation_indent
                avail = width - used
                chunk = text[i:i + avail]
                rows[-1].append((chunk, attr))
                used += len(chunk)
                i += len(chunk)

        for row in rows:
            try:
                win.scroll(1)
            except curses.error:
                pass
            y = h - 1
            x = 0
            for text, attr in row:
                if not text:
                    continue
                try:
                    win.addnstr(y, x, text, width - x, attr)
                except curses.error:
                    pass
                x += len(text)
                if x >= width:
                    break

    def _draw_frame(self, win, title: str, accent: int) -> None:
        win.erase()
        try:
            win.attron(accent)
            win.box()
            win.attroff(accent)
        except curses.error:
            pass
        label = f"┤ {title} ├"
        self._safe_addstr(win, 0, 2, label, accent | self.A_BOLD)

    def draw_header(self) -> None:
        self.hdr.erase()
        try:
            self.hdr.attron(self.A_ACCENT | self.A_BOLD)
            self.hdr.box()
            self.hdr.attroff(self.A_ACCENT | self.A_BOLD)
        except curses.error:
            pass

        if self.paused:
            spin = "⏸"
        else:
            spin = SPINNER[self.spinner_i % len(SPINNER)]
            self.spinner_i += 1

        base_disp = self.base
        if len(base_disp) > 50:
            base_disp = "…" + base_disp[-49:]
        with self._version_lock:
            ver = self._server_version
        ver_disp = f"v{ver}" if ver else "v?"
        title = f"┤ {spin}  MAPCODE STRESSTEST  →  {base_disp}  ·  {ver_disp} ├"
        title_attr = (self.A_WARN if self.paused else self.A_ACCENT) | self.A_BOLD
        self._safe_addstr(self.hdr, 0, 2, title, title_attr)
        if self.paused:
            badge = " ⏸ PAUSED "
            badge_x = 2 + len(title) + 1
            if badge_x + len(badge) < self.w - 2:
                self._safe_addstr(self.hdr, 0, badge_x, badge, self.A_WARN | self.A_BOLD | curses.A_REVERSE)

        with self.lock:
            o, e, c, t, r = (
                self.ok, self.err, self.cancelled,
                self.total_fired, self.last_rate,
            )
            recent_bursts = list(self._recent_bursts)
        in_flight = t - o - e - c
        elapsed = time.monotonic() - self.started
        avg_rate = t / elapsed if elapsed > 0 else 0.0
        burst_mean = sum(recent_bursts) / len(recent_bursts) if recent_bursts else 0.0
        tag = (
            f"┤ {self._fmt_duration(elapsed)}  ·  "
            f"target {self.args.rate:5.1f}/s  ·  "
            f"{r:5.1f}/s now  ·  {avg_rate:5.1f}/s avg  ·  "
            f"burst ≈ {burst_mean:4.1f} ├"
        )
        tag_x = max(2, self.w - 2 - len(tag))
        if tag_x > 2 + len(title) + 1:
            self._safe_addstr(self.hdr, 0, tag_x, tag, self.A_INFO)

        cols = [
            ("Sent",      f"{t:>6d}", self.A_INFO),
            ("OK",        f"{o:>6d}", self.A_OK),
            ("Error",     f"{e:>6d}", self.A_ERR),
            ("In-flight", f"{in_flight:>4d}", self.A_WARN),
            ("Cancelled", f"{c:>4d}", self.A_DIM),
        ]
        x = 2
        for label, val, attr in cols:
            seg = f" {label} "
            needed = len(seg) + len(val) + 2
            if x + needed >= self.w - 2:
                break  # Out of room — drop this column and any further ones.
            self._safe_addstr(self.hdr, 1, x, seg, self.A_DIM)
            x += len(seg)
            self._safe_addstr(self.hdr, 1, x, val, attr | self.A_BOLD)
            x += len(val) + 2

        total_done = o + e
        success_rate = (o / total_done) * 100 if total_done else 100.0

        bar_label = " success "
        self._safe_addstr(self.hdr, 2, 2, bar_label, self.A_DIM)
        bar_x = 2 + len(bar_label)
        pct_str = f" {success_rate:5.1f}%"
        bar_w = max(8, self.w - bar_x - len(pct_str) - 4)
        filled = int(round(bar_w * success_rate / 100))
        bar = "█" * filled + "░" * (bar_w - filled)
        if success_rate >= 99:
            bar_attr = self.A_OK | self.A_BOLD
        elif success_rate >= 95:
            bar_attr = self.A_OK
        elif success_rate >= 80:
            bar_attr = self.A_WARN
        else:
            bar_attr = self.A_ERR | self.A_BOLD
        self._safe_addstr(self.hdr, 2, bar_x, bar, bar_attr)
        self._safe_addstr(self.hdr, 2, bar_x + bar_w, pct_str, self.A_BOLD)

        self.hdr.noutrefresh()

    def draw_panes(self) -> None:
        # Borders are static (painted in _make_layout); only the inner
        # scrolling content needs to be flushed each frame.
        self.req_inner.noutrefresh()
        self.err_inner.noutrefresh()

    def draw_footer(self) -> None:
        self.foot.erase()
        if self.paused:
            # In-flight requests submitted before the pause will still
            # complete and write into the panes; surface how many are left.
            with self.lock:
                pending = self.total_fired - self.ok - self.err - self.cancelled
            if pending > 0:
                text = (
                    f"  ⏸ PAUSED — {pending} request(s) still in flight    ·    "
                    "any key resume    ·    [Ctrl-C] abort  "
                )
            else:
                text = (
                    "  ⏸ PAUSED — idle (no requests in flight)    ·    "
                    "any key resume    ·    [Ctrl-C] abort  "
                )
            attr = self.A_WARN | self.A_BOLD
        else:
            text = (
                "  [Esc] pause    ·    [+/-] rate ±1 req/s    "
                "·    [Ctrl-C] abort  "
            )
            attr = self.A_DIM
        self._safe_addstr(self.foot, 0, 0, text, attr)
        self.foot.noutrefresh()

    def emit_event(
        self,
        success: bool,
        code: int | None,
        msg: str | None,
        url: str,
        duration_ms: int,
        t_done: float,
    ) -> None:
        # Timestamp at completion, not at the moment we drained the queue.
        ts = time.strftime("%H:%M:%S", time.localtime(t_done))
        short = self._short(url)
        code_str = f"{code:>3}" if code is not None else " - "
        # Right-aligned ms field; 5 digits covers up to a 99,999 ms (~100 s)
        # response, more than the 10 s timeout we set on do_request.
        ms_str = f"{duration_ms:>5d} ms"
        # Colour the timing by how slow the call was.
        if duration_ms < 100:
            ms_attr = self.A_OK
        elif duration_ms < 500:
            ms_attr = self.A_INFO
        elif duration_ms < 2000:
            ms_attr = self.A_WARN
        else:
            ms_attr = self.A_ERR
        if success:
            self._add_line(
                self.req_inner,
                [
                    (f"{ts}  ", self.A_DIM),
                    (code_str, self._status_color(code) | self.A_BOLD),
                    ("  ", 0),
                    (ms_str, ms_attr),
                    ("  ✓  ", self.A_OK | self.A_BOLD),
                    (short, 0),
                ],
            )
        else:
            self._add_line(
                self.req_inner,
                [
                    (f"{ts}  ", self.A_DIM),
                    (code_str, self._status_color(code) | self.A_BOLD),
                    ("  ", 0),
                    (ms_str, ms_attr),
                    ("  ✗  ", self.A_ERR | self.A_BOLD),
                    (short, self.A_DIM),
                ],
            )
            self._add_line(
                self.err_inner,
                [
                    (f"{ts}  ", self.A_DIM),
                    (code_str, self._status_color(code) | self.A_BOLD),
                    ("  ✗ ", self.A_ERR | self.A_BOLD),
                    (msg or "unknown error", self.A_ERR | self.A_BOLD),
                    ("   ", 0),
                    (short, self.A_DIM),
                ],
            )

    def drain(self) -> None:
        any_event = False
        for _ in range(500):
            try:
                ev = self.events.get_nowait()
            except queue.Empty:
                break
            self.emit_event(*ev)
            any_event = True
        if any_event:
            self.req_inner.noutrefresh()
            self.err_inner.noutrefresh()

    def handle_keys(self) -> None:
        while True:
            ch = self.stdscr.getch()
            if ch == -1:
                return
            if ch == curses.KEY_RESIZE:
                self._make_layout()
                continue
            # Rate adjustment works in both running and paused states — these
            # keys are *not* treated as "any key" for resuming the pause.
            if ch in (ord("+"), ord("=")):
                self.args.rate = min(10000.0, self.args.rate + 1.0)
                continue
            if ch == ord("-"):
                self.args.rate = max(1.0, self.args.rate - 1.0)
                continue
            if self.paused:
                # Any other key resumes — including Esc.
                self.paused = False
                return
            if ch == 27:
                self.paused = True
                return
            # Other keys are ignored while running.

    def record(self, url: str, kind: str, fut) -> None:
        # Initialise everything up front so any control path produces a
        # well-formed event tuple.
        success: bool = False
        code: int | None = None
        msg: str | None = None
        body: bytes | None = None
        duration_ms: int = 0
        t_done = time.time()
        try:
            success, code, msg, body, duration_ms = fut.result()
        except CancelledError:
            with self.lock:
                self.cancelled += 1
            return
        except Exception as exc:  # noqa: BLE001 - the UI labels what we hit
            msg = f"{type(exc).__name__}: {exc}"
        with self.lock:
            if success:
                self.ok += 1
            else:
                self.err += 1
        if success and kind == "encode" and body:
            self._ingest_encode_body(body)
        self.events.put((success, code, msg, url, duration_ms, t_done))

    def _ingest_encode_body(self, body: bytes) -> None:
        try:
            payload = json.loads(body)
        except (ValueError, UnicodeDecodeError):
            return
        pairs = extract_mapcodes(payload)
        if not pairs:
            return
        with self._bucket_lock:
            self._bucket.extend(pairs)

    def _pick_decode_target(self) -> tuple[str, str | None]:
        with self._bucket_lock:
            n = len(self._bucket)
            if n:
                return self._bucket[random.randrange(n)]
        return random.choice(SAMPLE_MAPCODES)

    def _next_url_and_kind(self) -> tuple[str, str]:
        # While the bucket is still small, lean heavily on encode requests to
        # seed it. Once it reaches the warmup target the mix settles to 50/50.
        # We interpolate so the regime change isn't a visible cliff.
        with self._bucket_lock:
            bkt = len(self._bucket)
        warmup_target = max(100, int(self.args.rate * 20))
        ratio = min(1.0, bkt / warmup_target) if warmup_target else 1.0
        encode_prob = 0.9 - 0.4 * ratio  # 0.9 → 0.5 as bucket fills
        if random.random() < encode_prob:
            return random_encode_url(self.base), "encode"
        mc, ctx = self._pick_decode_target()
        return decode_url(self.base, mc, ctx), "decode"

    def _version_poller(self) -> None:
        """Background heartbeat: hit /mapcode/version every 10 seconds.

        Runs in a daemon thread and does not push events into the request/error
        panes — its job is just to keep `self._server_version` current for the
        header. Uses its own thread-local connection thanks to `do_request`.
        """
        url = self.base + "/mapcode/version"
        while not self._version_stop.is_set():
            ver: str | None = None
            try:
                success, _code, _msg, body, _dur = do_request(url)
                if success and body:
                    try:
                        payload = json.loads(body)
                    except (ValueError, UnicodeDecodeError):
                        payload = None
                    if isinstance(payload, dict):
                        v = payload.get("version")
                        if isinstance(v, str) and v:
                            ver = v
                    if ver is None:
                        txt = body.decode("utf-8", "replace").strip()
                        if txt:
                            ver = txt[:32]
            except Exception:  # noqa: BLE001 - background poller mustn't crash the UI
                ver = None
            with self._version_lock:
                self._server_version = ver
            self._version_stop.wait(10.0)

    def _pump_ui(self) -> None:
        """Process input + redraw a single frame."""
        self.handle_keys()
        if self._layout_too_small:
            # Show a minimal placeholder and keep buffering events in the
            # queue until the user resizes back to a usable size.
            try:
                self.stdscr.erase()
                msg = "Terminal too small — resize to at least 60x14"
                if self.w > 0 and self.h > 0:
                    self.stdscr.addnstr(0, 0, msg, max(0, self.w - 1))
                self.stdscr.refresh()
            except curses.error:
                pass
            return
        self.drain()
        self.draw_header()
        self.draw_panes()
        self.draw_footer()
        curses.doupdate()

    def run(self) -> None:
        args = self.args

        # Pool is sized once, based on the *initial* rate. If the user later
        # bumps --rate at runtime via +/-, the executor just keeps tasks in
        # its internal queue once it hits the worker cap — acceptable for a
        # stress tool.
        initial_rate = args.rate
        expected_in_flight = int(initial_rate * REQUEST_TIMEOUT)
        max_workers = min(256, max(8, expected_in_flight * 2))
        ex = ThreadPoolExecutor(max_workers=max_workers)
        version_thread = threading.Thread(
            target=self._version_poller, name="version-poller", daemon=True
        )
        version_thread.start()
        try:
            while True:
                if args.total_requests is not None and self.total_fired >= args.total_requests:
                    break

                # Hold here while paused; new bursts wait until resumed.
                while self.paused:
                    self._pump_ui()
                    time.sleep(0.05)

                # Re-read the target rate every iteration so the +/- key
                # handlers take effect on the very next burst.
                #
                # Compound-Poisson-ish pacing:
                #   - mean burst size grows sub-linearly with the target rate
                #   - actual burst size: max(1, ~Exponential(mean_burst)), capped
                #   - inter-burst delay: (burst / target_rate) * uniform jitter
                # E[burst] / E[delay] = target_rate as long as E[jitter] == 1.
                target_rate = args.rate
                mean_burst = min(50.0, max(1.0, target_rate / 3.0))
                burst_cap = max(2, int(mean_burst * 8))

                burst = max(1, min(burst_cap, int(random.expovariate(1.0 / mean_burst))))
                if args.total_requests is not None:
                    burst = min(burst, args.total_requests - self.total_fired)
                if burst <= 0:
                    break

                # Update counters BEFORE submitting so a worker that finishes
                # instantly cannot make `in_flight = total_fired - ok - err - c`
                # transiently negative.
                with self.lock:
                    self.total_fired += burst
                    self.fired_this_window += burst
                    self._recent_bursts.append(burst)
                    now = time.monotonic()
                    window = now - self.window_start
                    if window >= 1.0:
                        self.last_rate = self.fired_this_window / window
                        self.fired_this_window = 0
                        self.window_start = now

                for _ in range(burst):
                    url, kind = self._next_url_and_kind()
                    fut = ex.submit(do_request, url)
                    fut.add_done_callback(
                        lambda f, u=url, k=kind: self.record(u, k, f)
                    )

                # Jittered wait so the schedule is not perfectly periodic.
                # E[jitter] == 1 keeps the long-run rate at target_rate.
                jitter = random.uniform(0.3, 1.7)
                delay = (burst / target_rate) * jitter

                target_t = time.monotonic() + delay
                while time.monotonic() < target_t:
                    if args.total_requests is not None and self.total_fired >= args.total_requests:
                        break
                    self._pump_ui()
                    if self.paused:
                        # Stop the inter-burst timer and fall into the outer
                        # pause hold immediately.
                        break
                    time.sleep(min(0.05, max(0.005, (target_t - time.monotonic()) / 4)))
        except KeyboardInterrupt:
            self.aborted = True
        finally:
            self._version_stop.set()
            ex.shutdown(wait=False, cancel_futures=True)
            self.drain()
            self.draw_header()
            self.draw_panes()
            self.draw_footer()
            curses.doupdate()


def _run(stdscr, args) -> "UI":
    ui = UI(stdscr, args)
    ui.run()
    return ui


def main() -> int:
    locale.setlocale(locale.LC_ALL, "")
    p = argparse.ArgumentParser(description="Stresstest the mapcode API.")
    p.add_argument("--url", default="http://localhost:8080",
                   help="Base URL of the mapcode API (default: %(default)s). "
                        "Point at a local instance; passing a public host hammers real users.")
    p.add_argument("--rate", type=float, default=10.0,
                   help="Target average request rate in req/s (default: %(default)s). "
                        "Bursts and inter-burst delays are randomized; the long-run "
                        "average converges to this value.")
    p.add_argument("--total-requests", type=int, default=None,
                   help="Stop after this many total requests (default: unlimited)")
    p.add_argument("--bucket-size", type=int, default=10000,
                   help="Rolling FIFO size of valid mapcodes harvested from "
                        "encode responses; decode requests sample from it "
                        "(default: %(default)s)")
    args = p.parse_args()

    if not math.isfinite(args.rate) or args.rate <= 0:
        print("--rate must be a positive, finite number", file=sys.stderr)
        return 2
    if args.rate < 0.01 or args.rate > 10000:
        print("--rate must be between 0.01 and 10000 req/s", file=sys.stderr)
        return 2
    if args.bucket_size < 1:
        print("--bucket-size must be >= 1", file=sys.stderr)
        return 2

    try:
        ui = curses.wrapper(_run, args)
    except RuntimeError as e:
        print(str(e), file=sys.stderr)
        return 2

    if not ui.aborted:
        elapsed = time.monotonic() - ui.started
        unfinished = ui.total_fired - ui.ok - ui.err - ui.cancelled
        print(
            f"Done. fired={ui.total_fired} in {elapsed:.1f}s "
            f"-- OK={ui.ok}, ERR={ui.err}, cancelled={ui.cancelled}, "
            f"unfinished={unfinished}"
        )
    return 0


if __name__ == "__main__":
    sys.exit(main())
