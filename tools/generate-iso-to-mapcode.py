#!/usr/bin/env python3
"""Generate tools/iso-to-mapcode.json from the mapcode library's Territory enum.

The mapcode Java library (com.mapcode:mapcode) is the source of truth for
which mapcode alphaCodes exist and how they relate to ISO 3166 codes. This
script parses Territory.java directly from the library's sources jar
(downloaded into ~/.m2 by Maven) and emits the JSON mapping consumed by
tools/build-borders.py.

For each Territory it emits:
  * The canonical alphaCode entry (enum name with '_' replaced by '-').
  * Extra alias entries for ISO-3166-shaped aliases on the same Territory
    (e.g. HKG also gets an alias entry under CN-HK), so OSM tags that use
    the country-prefixed form still resolve to the canonical alphaCode.

Regenerate after bumping the mapcode.version property in pom.xml:

    python3 tools/generate-iso-to-mapcode.py
"""

from __future__ import annotations

import json
import re
import sys
import zipfile
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
POM = REPO_ROOT / "pom.xml"
OUT_PATH = REPO_ROOT / "tools" / "iso-to-mapcode.json"

# Aliases shaped like ISO 3166-1 alpha-3 ("HKG", "JTN") or
# ISO 3166-2 alpha-2 + subdivision ("CN-HK", "US-UM").
ISO_ALPHA3_RE = re.compile(r"^[A-Z]{3}$")
ISO_ALPHA2_SUB_RE = re.compile(r"^[A-Z]{2}-[A-Z0-9]{1,4}$")


def mapcode_version() -> str:
    text = POM.read_text()
    m = re.search(r"<mapcode\.version>([^<]+)</mapcode\.version>", text)
    if not m:
        sys.exit(f"Could not find <mapcode.version> in {POM}")
    return m.group(1)


def territory_java(version: str) -> str:
    home = Path.home()
    jar = home / ".m2/repository/com/mapcode/mapcode" / version / f"mapcode-{version}-sources.jar"
    if not jar.exists():
        sys.exit(
            f"Mapcode sources jar not found: {jar}\n"
            f"Run `mvn -pl service dependency:resolve-sources -DincludeArtifactIds=mapcode` first."
        )
    with zipfile.ZipFile(jar) as z:
        with z.open("com/mapcode/Territory.java") as f:
            return f.read().decode("utf-8")


def split_args(s: str) -> list[str]:
    """Split a Java argument list on top-level commas, respecting strings and brackets."""
    args: list[str] = []
    cur: list[str] = []
    depth = 0
    in_str = False
    esc = False
    for c in s:
        if in_str:
            cur.append(c)
            if esc:
                esc = False
            elif c == "\\":
                esc = True
            elif c == '"':
                in_str = False
            continue
        if c == '"':
            in_str = True
            cur.append(c)
        elif c in "([{":
            depth += 1
            cur.append(c)
        elif c in ")]}":
            depth -= 1
            cur.append(c)
        elif c == "," and depth == 0:
            args.append("".join(cur).strip())
            cur = []
        else:
            cur.append(c)
    tail = "".join(cur).strip()
    if tail:
        args.append(tail)
    return args


ENTRY_HEAD_RE = re.compile(r"^    ([A-Z][A-Z0-9_]*)\(", re.MULTILINE)
STRING_LITERAL_RE = re.compile(r'"([^"\\]*(?:\\.[^"\\]*)*)"')


def parse_territories(java_src: str) -> list[dict]:
    """Return [{name, parent, aliases}] for every Territory enum entry.

    Most entries sit on a single line, but a few span two lines when their
    fullNameAliases push the entry past the column limit. We locate each
    entry by its leading "    NAME(" and then read forward, balancing
    brackets and skipping string contents, until the matching ')'.
    """
    enum_open = java_src.index("public enum Territory")
    body_start = java_src.index("{", enum_open) + 1
    body_end = java_src.index("Territory(final int number", body_start)
    body = java_src[body_start:body_end]

    territories: list[dict] = []
    for m in ENTRY_HEAD_RE.finditer(body):
        name = m.group(1)
        i = m.end()  # just past the '('
        depth = 1
        in_str = False
        esc = False
        start = i
        while i < len(body):
            c = body[i]
            if in_str:
                if esc:
                    esc = False
                elif c == "\\":
                    esc = True
                elif c == '"':
                    in_str = False
            elif c == '"':
                in_str = True
            elif c in "([{":
                depth += 1
            elif c in ")]}":
                depth -= 1
                if depth == 0:
                    break
            i += 1
        if depth != 0:
            sys.exit(f"Unbalanced parens parsing entry {name}")
        args = split_args(body[start:i])
        # Constructor positional args: number, fullName, [alphabets], [parent],
        # [aliases], [fullNameAliases]. parent is the 4th arg, aliases the 5th.
        parent = None
        aliases: list[str] = []
        if len(args) >= 4 and args[3] != "null":
            parent = args[3]
        if len(args) >= 5 and args[4] != "null":
            aliases = STRING_LITERAL_RE.findall(args[4])
        territories.append({"name": name, "parent": parent, "aliases": aliases})
    return territories


def alpha_code(enum_name: str) -> str:
    return enum_name.replace("_", "-")


def build_mapping(territories: list[dict], version: str) -> dict:
    mapping: dict = {
        "_comment": (
            f"Generated by tools/generate-iso-to-mapcode.py from mapcode "
            f"{version}. Do not edit by hand. Keys are ISO 3166-1 alpha-3 "
            f"(countries) or ISO 3166-2 (subdivisions). Values are mapcode "
            f"alphaCodes."
        ),
    }

    # First pass: canonical alphaCode for every Territory.
    for t in territories:
        key = alpha_code(t["name"])
        entry: dict = {"alphaCode": key}
        if t["parent"]:
            entry["parentAlphaCode"] = alpha_code(t["parent"])
        mapping[key] = entry

    # Second pass: ISO-shaped aliases. Skip aliases that collide with a
    # canonical key (the canonical entry wins).
    for t in territories:
        target_alpha = alpha_code(t["name"])
        target_parent = alpha_code(t["parent"]) if t["parent"] else None
        for alias in t["aliases"]:
            if not (ISO_ALPHA3_RE.match(alias) or ISO_ALPHA2_SUB_RE.match(alias)):
                continue
            if alias in mapping:
                continue
            entry = {"alphaCode": target_alpha}
            if target_parent:
                entry["parentAlphaCode"] = target_parent
            mapping[alias] = entry

    return mapping


def dump_json(mapping: dict, path: Path) -> None:
    """Write JSON with one entry per line for readable diffs."""
    keys = list(mapping.keys())
    # Keep _comment first, then sort the rest alphabetically.
    keys.remove("_comment")
    keys.sort()
    lines = ["{"]
    lines.append(f'  "_comment": {json.dumps(mapping["_comment"])},')
    for i, k in enumerate(keys):
        suffix = "," if i < len(keys) - 1 else ""
        lines.append(f"  {json.dumps(k)}: {json.dumps(mapping[k])}{suffix}")
    lines.append("}")
    path.write_text("\n".join(lines) + "\n")


def main() -> None:
    version = mapcode_version()
    src = territory_java(version)
    territories = parse_territories(src)
    if not territories:
        sys.exit("Parsed zero Territory entries; aborting.")
    mapping = build_mapping(territories, version)
    dump_json(mapping, OUT_PATH)
    # _comment plus all keys
    print(
        f"Wrote {OUT_PATH.relative_to(REPO_ROOT)} "
        f"({len(territories)} territories, {len(mapping) - 1} keys including aliases) "
        f"from mapcode {version}.",
        file=sys.stderr,
    )


if __name__ == "__main__":
    main()
