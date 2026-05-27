#!/usr/bin/env python3
"""Build the production borders FlatGeobuf from OSM admin-boundary data.

Pipeline:
  1. Read OSM admin_level 2 (countries) and admin_level 4 (first-level
     subdivisions) polygons from the input source.
  2. Resolve each polygon's mapcode alphaCode from its ISO 3166 tags:
       - admin_level 2 -> ISO3166-1:alpha3, looked up against the
         iso-to-mapcode table.
       - admin_level 4 -> ISO3166-2 (e.g., "US-CA"); if no mapcode
         equivalent exists, fall back to the parent country's
         alphaCode. If neither, drop the polygon.
  3. Simplify each polygon with Douglas-Peucker (tolerance ~50 m, i.e.
     ~0.00045 deg at the equator; configurable via --tolerance).
  4. Compute polygon area (square degrees is fine; only used as a tie-
     breaker, not displayed).
  5. Write FlatGeobuf with feature properties:
       alphaCode (string), parentAlphaCode (string, "" when absent),
       adminLevel (int), area (double).

The iso-to-mapcode mapping is loaded from --mapping (a JSON file). The
file is hand-maintained for now; a future Maven exec target may export
it from the mapcode library.

Input formats:
  - `.geojsonseq` (GeoJSON Text Sequence): streamed line-by-line with a
    progress indicator that reports bytes read and features processed.
    Recommended for multi-GB planet extracts because `gpd.read_file()`
    on a large GeoJSON is opaque (no streaming API in pyogrio 0.12.1).

    Produce it once from your admin-only PBF with:
        osmium export --geometry-types=polygon -f geojsonseq \\
            -o admin.geojsonseq admin.osm.pbf

  - Anything else (`.geojson`, `.osm.pbf`, ...): loaded in one shot via
    `gpd.read_file()`. Fine for small inputs; silent on big ones.

Run:
    python3 tools/build-borders.py \\
        --osm <osm.pbf-or-geojson-or-geojsonseq> \\
        --mapping tools/iso-to-mapcode.json \\
        --out borders.fgb \\
        [--tolerance 0.00045]
"""
import argparse
import json
import sys
from pathlib import Path

import geopandas as gpd
from shapely.geometry import shape as shape_from_geojson


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(
        description=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    p.add_argument("--osm", required=True,
                   help="Path to an OSM extract: .geojsonseq (streamed with "
                        "progress) or any format gpd.read_file accepts "
                        "(.geojson, .osm.pbf, ...). Must contain "
                        "admin_level 2 and 4 boundary polygons.")
    p.add_argument("--mapping", required=True,
                   help="Path to a JSON file mapping ISO codes to mapcode "
                        "alphaCodes. Format: "
                        "{\"USA\": {\"alphaCode\": \"USA\"}, "
                        "\"US-CA\": {\"alphaCode\": \"USA-CA\", "
                        "\"parentAlphaCode\": \"USA\"}, ...}")
    p.add_argument("--out", required=True,
                   help="Output FlatGeobuf path.")
    p.add_argument("--tolerance", type=float, default=0.00045,
                   help="Douglas-Peucker simplification tolerance, degrees. "
                        "Default ~50 m at the equator.")
    return p.parse_args()


def load_mapping(path: Path) -> dict:
    with open(path) as f:
        return json.load(f)


def resolve_alpha_code(iso: str, admin_level: int, mapping: dict):
    """Return (alphaCode, parentAlphaCode) or None to drop the polygon.

    For admin_level 4: try the ISO-3166-2 code; if missing in the mapping,
    fall back to the parent country's mapcode alphaCode (derived from the
    leading characters of the ISO-3166-2 code). If neither exists, return
    None.
    """
    entry = mapping.get(iso)
    if entry is not None:
        return entry["alphaCode"], entry.get("parentAlphaCode")
    if admin_level == 4 and "-" in iso:
        country = iso.split("-", 1)[0]
        parent_entry = mapping.get(country)
        if parent_entry is not None:
            return parent_entry["alphaCode"], None
    return None


class _ByteProgress:
    """Periodic progress printer keyed on bytes consumed.

    Emits one line per `step_pct` of total size (default 1%). When stderr
    is a TTY, the line is rewritten in place; otherwise each tick is a
    separate line so the output is friendly to log files.
    """

    def __init__(self, total: int, step_pct: float = 1.0,
                 label: str = "  read"):
        self.total = max(total, 1)
        self.step = max(self.total * step_pct / 100.0, 1)
        self.label = label
        self.tty = sys.stderr.isatty()
        self.bytes = 0
        self.features = 0
        self.next_tick = self.step

    def advance(self, nbytes: int) -> None:
        self.bytes += nbytes
        if self.bytes >= self.next_tick:
            self._emit()
            while self.next_tick <= self.bytes:
                self.next_tick += self.step

    def mark_kept(self) -> None:
        self.features += 1

    def finish(self) -> None:
        self._emit()
        if self.tty:
            print("", file=sys.stderr, flush=True)

    def _emit(self) -> None:
        pct = 100.0 * self.bytes / self.total
        msg = (f"{self.label} {self.bytes / 1e9:6.2f}/"
               f"{self.total / 1e9:.2f} GB ({pct:5.1f}%) "
               f"— {self.features} features")
        if self.tty:
            print(f"\r{msg}", end="", file=sys.stderr, flush=True)
        else:
            print(msg, file=sys.stderr, flush=True)


def iter_features_streaming(path: Path):
    """Yield (geometry, properties) for admin_level 2/4 features only.

    Reports byte progress so multi-GB inputs are no longer opaque. Handles
    RFC 7464 record-separator framing (lines may start with 0x1E) and
    skips blank lines. JSON / geometry parse errors are silently dropped.

    The admin_level filter is applied *before* constructing the Shapely
    geometry, because an OSM r/boundary=administrative extract contains
    hundreds of thousands of low-level features (admin_level 6/8/9/10)
    that we never use — building shapely polygons for all of them would
    dwarf the actual work.
    """
    size = path.stat().st_size
    print(f"Loading OSM data (streaming {size / 1e9:.2f} GB) ...",
          file=sys.stderr)
    progress = _ByteProgress(size)
    try:
        with open(path, "rb") as f:
            for raw in f:
                progress.advance(len(raw))
                line = raw.lstrip(b"\x1e").strip()
                if not line:
                    continue
                try:
                    feat = json.loads(line)
                except json.JSONDecodeError:
                    continue
                props = feat.get("properties") or {}
                # Cheap pre-filter: OSM admin_level is a string tag, but
                # be liberal and accept ints too.
                level_raw = props.get("admin_level")
                if level_raw not in ("2", "4", 2, 4):
                    continue
                geom_obj = feat.get("geometry")
                if geom_obj is None:
                    continue
                try:
                    geom = shape_from_geojson(geom_obj)
                except Exception:
                    continue
                progress.mark_kept()
                yield geom, props
    finally:
        progress.finish()


def iter_features_geopandas(path: Path):
    """Yield (geometry, properties) by reading the whole file via gpd.

    Loads everything into memory at once. Fine for small inputs; for
    multi-GB inputs prefer the `.geojsonseq` streaming path.
    """
    print("Loading OSM data ...", file=sys.stderr)
    gdf = gpd.read_file(path)
    gdf = gdf[gdf["admin_level"].isin([2, 4])]
    total = len(gdf)
    w = len(str(total))
    print(f"Processing {total} polygons ...", file=sys.stderr)
    for i, (_, row) in enumerate(gdf.iterrows(), start=1):
        if total > 0 and (i % 250 == 0 or i == total):
            pct = 100 * i // total
            print(f"  Processed {i:{w}}/{total} polygons ({pct:3d}%)",
                  file=sys.stderr)
        yield row.geometry, row


def main() -> int:
    args = parse_args()
    mapping = load_mapping(Path(args.mapping))
    osm_path = Path(args.osm)

    if osm_path.suffix.lower() == ".geojsonseq":
        feature_iter = iter_features_streaming(osm_path)
    else:
        feature_iter = iter_features_geopandas(osm_path)

    rows = []
    written = dropped = 0
    for geom, props in feature_iter:
        level_raw = props.get("admin_level")
        if level_raw is None:
            continue
        try:
            level = int(level_raw)
        except (ValueError, TypeError):
            continue
        if level not in (2, 4):
            continue
        iso = (props.get("ISO3166-1:alpha3") if level == 2
               else props.get("ISO3166-2"))
        if iso is None:
            dropped += 1
            continue
        resolved = resolve_alpha_code(iso, level, mapping)
        if resolved is None:
            dropped += 1
            continue
        alpha, parent = resolved
        simplified = geom.simplify(args.tolerance, preserve_topology=True)
        if simplified.is_empty:
            dropped += 1
            continue
        rows.append({
            "geometry": simplified,
            "alphaCode": alpha,
            "parentAlphaCode": parent or "",
            "adminLevel": level,
            "area": simplified.area,
        })
        written += 1

    print(f"Writing FlatGeobuf ({written} polygons) ...", file=sys.stderr)
    out_gdf = gpd.GeoDataFrame(rows, crs="EPSG:4326")
    out_gdf.to_file(args.out, driver="FlatGeobuf")

    print(f"Wrote {written} polygons to {args.out}; dropped {dropped}.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
