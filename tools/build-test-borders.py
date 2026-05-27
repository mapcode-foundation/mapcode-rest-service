#!/usr/bin/env python3
"""Build a tiny synthetic borders FlatGeobuf used by unit tests.

Polygons:
  - NLD            country, square covering roughly the Netherlands.
  - USA            country, square covering the contiguous US.
  - USA-CA         subdivision inside USA, square covering California.
  - DISPUTED-A     country square overlapping DISPUTED-B (simulates a disputed region).
  - DISPUTED-B     country square overlapping DISPUTED-A; smaller area so it ranks first.
  - NO-MAPCODE-PARENT (admin 2 + 4) simulates a subdivision whose alphaCode has
                   already been collapsed to the parent country by the prod build script.

Run:
    python3 tools/build-test-borders.py
Output:
    service/src/test/resources/borders-test.fgb

Requirements:
    pip install geopandas shapely
"""
import pathlib
import geopandas as gpd
from shapely.geometry import Polygon

OUT = pathlib.Path(__file__).resolve().parent.parent / "service" / "src" / "test" / "resources" / "borders-test.fgb"
OUT.parent.mkdir(parents=True, exist_ok=True)

FEATURES = [
    ("NLD",               "",    2, Polygon([(3.0, 50.5), (7.5, 50.5), (7.5, 53.7), (3.0, 53.7)])),
    ("USA",               "",    2, Polygon([(-125.0, 24.0), (-66.0, 24.0), (-66.0, 49.0), (-125.0, 49.0)])),
    ("USA-CA",            "USA", 4, Polygon([(-124.0, 32.5), (-114.0, 32.5), (-114.0, 42.0), (-124.0, 42.0)])),
    ("DISPUTED-A",        "",    2, Polygon([(100.0, 0.0), (110.0, 0.0), (110.0, 10.0), (100.0, 10.0)])),
    ("DISPUTED-B",        "",    2, Polygon([(105.0, 5.0), (108.0, 5.0), (108.0, 8.0), (105.0, 8.0)])),
    ("NO-MAPCODE-PARENT", "",    2, Polygon([(20.0, 60.0), (25.0, 60.0), (25.0, 65.0), (20.0, 65.0)])),
    ("NO-MAPCODE-PARENT", "",    4, Polygon([(21.0, 61.0), (24.0, 61.0), (24.0, 64.0), (21.0, 64.0)])),
]

rows = []
for alpha, parent, level, poly in FEATURES:
    rows.append({
        "alphaCode":       alpha,
        "parentAlphaCode": parent,
        "adminLevel":      level,
        "area":            poly.area,
        "geometry":        poly,
    })

gdf = gpd.GeoDataFrame(rows, crs="EPSG:4326")
# Ensure correct dtypes so FlatGeobuf schema round-trips cleanly.
gdf["adminLevel"] = gdf["adminLevel"].astype("int32")
gdf["area"]       = gdf["area"].astype("float64")

gdf.to_file(OUT, driver="FlatGeobuf")
print(f"Wrote {OUT}  ({OUT.stat().st_size} bytes, {len(gdf)} features)")
