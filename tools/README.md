# Tools

Operator scripts for the mapcode REST service.

## build-borders.py — production borders dataset

Produces the `borders.fgb` file consumed by the service at runtime
(via the `mapcode.borders.path` system property).

Pipeline:
1. Pull OSM admin_level 2 and 4 boundary polygons.
2. Read `ISO3166-1:alpha3` (countries) and `ISO3166-2` (subdivisions) tags.
3. Map ISO codes to mapcode `Territory` `alphaCode`s.
4. Apply the fallback rule: if a subdivision has no mapcode equivalent,
   tag it with the parent country's `alphaCode`. If neither subdivision
   nor country has a mapcode equivalent, drop the polygon.
5. Simplify with Douglas-Peucker (tolerance ~50 m).
6. Compute polygon area; write FlatGeobuf with properties:
   `alphaCode`, `parentAlphaCode`, `adminLevel`, `area`.

Run:
    python3 tools/build-borders.py --osm <osm-pbf-or-source> --out borders.fgb

Then upload the resulting file to wherever deployments fetch it from
and point the service at it via `-Dmapcode.borders.path=/path/to/borders.fgb`.

## build-test-borders.py — synthetic test fixture

Regenerates `service/src/test/resources/borders-test.fgb` from a small
hand-built set of polygons. Re-run only when the fixture needs to change.

Run:
    python3 tools/build-test-borders.py

Requirements: see the head of each script. Typically `pip install geopandas shapely` or equivalent.
