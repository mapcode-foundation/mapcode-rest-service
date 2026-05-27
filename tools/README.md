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

The script requires an `iso-to-mapcode.json` mapping file (ISO 3166 codes → mapcode
`alphaCode` strings). The full mapping is committed at `tools/iso-to-mapcode.json` and
covers every territory in the mapcode library declared in `pom.xml`.

Run:
    `python3 tools/build-borders.py --osm <osm-pbf-or-source> --out borders.fgb --mapping tools/iso-to-mapcode.json`

### Regenerating iso-to-mapcode.json

After bumping `mapcode.version` in `pom.xml`, regenerate the mapping from the new
library's `Territory` enum:

    `mvn -pl service dependency:resolve-sources -DincludeArtifactIds=mapcode -q`
    `python3 tools/generate-iso-to-mapcode.py`

The generator parses `Territory.java` from the mapcode sources jar in `~/.m2` and
emits canonical alphaCodes plus ISO-3166-shaped aliases (e.g. `CN-HK` → `HKG`).

Then upload the resulting file to wherever deployments fetch it from
and point the service at it via `-Dmapcode.borders.path=/path/to/borders.fgb`.

## build-test-borders.py — synthetic test fixture

Regenerates `service/src/test/resources/borders-test.fgb` from a small
hand-built set of polygons. Re-run only when the fixture needs to change.

Run: 
    `python3 tools/build-test-borders.py`

Requirements: see the head of each script. Typically `pip install geopandas shapely` or equivalent.
