# Tools

Operator scripts for the mapcode REST service.

## build-borders.py — production borders dataset

Produces the `borders.fgb` file consumed by the service at runtime
(via the `mapcode.borders.path` system property / `MAPCODE_BORDERS_PATH` env var).

What it does:
1. Reads OSM admin_level 2 (countries) and admin_level 4 (first-level
   subdivisions) boundary polygons from the input.
2. Reads `ISO3166-1:alpha3` (countries) and `ISO3166-2` (subdivisions) tags.
3. Maps ISO codes to mapcode `Territory` `alphaCode`s using
   `tools/iso-to-mapcode.json`.
4. Applies the fallback rule: if a subdivision has no mapcode equivalent,
   tag it with the parent country's `alphaCode`. If neither subdivision
   nor country has a mapcode equivalent, drop the polygon.
5. Simplifies with Douglas-Peucker (tolerance ~50 m, configurable via
   `--tolerance`).
6. Writes FlatGeobuf with feature properties:
   `alphaCode`, `parentAlphaCode`, `adminLevel`, `area`.

### Requirements

- Python 3 with `geopandas`, `shapely`, `pyogrio` (or `fiona`) installed.
  `pip install geopandas shapely pyogrio` covers it.
- [`osmium-tool`](https://osmcode.org/osmium-tool/) for extracting admin
  boundaries from a planet PBF (`brew install osmium-tool` on macOS).
- Roughly 110 GB of free disk to hold the planet PBF (~93 GB), the
  filtered admin-only PBF (~1 GB), and the GeoJSON Text Sequence
  intermediate (~10 GB). The output `borders.fgb` itself is on the
  order of tens of MB.

### End-to-end workflow

The full path from "nothing" to a working `borders.fgb`:

1. **Download the OSM planet PBF** (one-time, ~93 GB):

       curl -L -C - -o ~/Downloads/planet-latest.osm.pbf \
           https://planet.openstreetmap.org/pbf/planet-latest.osm.pbf

   `curl -C -` lets you resume if the connection drops.

2. **Filter to admin-boundary relations only** (~1 GB output):

       osmium tags-filter --progress \
           -o ~/Downloads/admin.osm.pbf --overwrite \
           ~/Downloads/planet-latest.osm.pbf \
           r/boundary=administrative

   The planet PBF can be deleted after this step if disk is tight.

3. **Export polygons as GeoJSON Text Sequence** (~10 GB output, one
   feature per line — this format is what gives `build-borders.py` a
   real progress indicator on the load step; ordinary `.geojson` of this
   size is opaque to `gpd.read_file()`):

       osmium export --progress --geometry-types=polygon \
           -f geojsonseq \
           -o ~/Downloads/admin.geojsonseq --overwrite \
           ~/Downloads/admin.osm.pbf

4. **Build the FlatGeobuf:**

       python3 tools/build-borders.py \
           --osm ~/Downloads/admin.geojsonseq \
           --mapping tools/iso-to-mapcode.json \
           --out ~/Downloads/borders.fgb

   Progress output looks like:

       Loading OSM data (streaming 9.71 GB) ...
         read   0.10/9.71 GB (  1.0%) — 12 features
         read   0.20/9.71 GB (  2.1%) — 38 features
         ...
       Writing FlatGeobuf (3742 polygons) ...
       Wrote 3742 polygons to ~/Downloads/borders.fgb; dropped 211.

   "features" counts admin_level 2/4 polygons seen so far; non-matching
   admin_levels are skipped cheaply before geometry construction. The
   final "dropped" count covers level 2/4 polygons whose ISO tag could
   not be resolved against `iso-to-mapcode.json`.

5. **Point the service at the output:**

       export MAPCODE_BORDERS_PATH=~/Downloads/borders.fgb
       # ... start the service per the top-level README ...

### Input format detection

The script picks its loader from the input file suffix:

| Suffix         | Loader                            | Use when                           |
|----------------|-----------------------------------|------------------------------------|
| `.geojsonseq`  | streaming, per-line, with progress | planet-scale or any multi-GB input |
| anything else  | `gpd.read_file()` (one shot)      | small inputs (test fixtures, etc.) |

### Tuning

- `--tolerance` (default `0.00045` ≈ 50 m at the equator) controls
  Douglas-Peucker simplification. Increase for smaller / coarser output,
  decrease for higher-fidelity (and larger) borders.

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
