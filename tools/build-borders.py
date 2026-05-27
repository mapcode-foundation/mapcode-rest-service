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

Run:
    python3 tools/build-borders.py \\
        --osm <osm.pbf-or-geojson> \\
        --mapping tools/iso-to-mapcode.json \\
        --out borders.fgb \\
        [--tolerance 0.00045]
"""
import argparse
import json
import sys
from pathlib import Path

import geopandas as gpd
from shapely.geometry import mapping as geom_to_mapping


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(
        description=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    p.add_argument("--osm", required=True,
                   help="Path to an OSM extract (e.g., .osm.pbf or GeoJSON) "
                        "with admin_level 2 and 4 boundary polygons.")
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


def main() -> int:
    args = parse_args()
    mapping = load_mapping(Path(args.mapping))

    print("Loading OSM data ...", file=sys.stderr)
    gdf = gpd.read_file(args.osm)
    gdf = gdf[gdf["admin_level"].isin([2, 4])]

    total = len(gdf)
    w = len(str(total))
    print(f"Processing {total} polygons ...", file=sys.stderr)

    rows = []
    written = dropped = 0
    for i, (_, row) in enumerate(gdf.iterrows(), start=1):
        if total > 0 and (i % 250 == 0 or i == total):
            pct = 100 * i // total
            print(f"  Processed {i:{w}}/{total} polygons ({pct:3d}%)", file=sys.stderr)
        level = int(row["admin_level"])
        iso = row.get("ISO3166-1:alpha3") if level == 2 else row.get("ISO3166-2")
        if iso is None:
            dropped += 1
            continue
        resolved = resolve_alpha_code(iso, level, mapping)
        if resolved is None:
            dropped += 1
            continue
        alpha, parent = resolved
        geom = row.geometry.simplify(args.tolerance, preserve_topology=True)
        if geom.is_empty:
            dropped += 1
            continue
        rows.append({
            "geometry": geom,
            "alphaCode": alpha,
            "parentAlphaCode": parent or "",
            "adminLevel": level,
            "area": geom.area,
        })
        written += 1

    print("Writing FlatGeobuf ...", file=sys.stderr)
    out_gdf = gpd.GeoDataFrame(rows, crs=gdf.crs)
    out_gdf.to_file(args.out, driver="FlatGeobuf")

    print(f"Wrote {written} polygons to {args.out}; dropped {dropped}.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
