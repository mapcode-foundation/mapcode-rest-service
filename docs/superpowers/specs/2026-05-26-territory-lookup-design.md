# Territory Lookup by Lat/Lon — Design

**Status:** Draft for review
**Date:** 2026-05-26
**Author:** Brainstormed with Claude

## Goal

Add a REST endpoint that returns the "most likely" territories for a given
lat/lon, ranked. "Most likely" reflects two realities:

1. Country and subdivision borders are not 100% accurate.
2. Some borders are geopolitically sensitive (disputed regions, overlapping
   claims).

Rather than the mapcode library's built-in territory lookup, the endpoint is
backed by OSM admin-boundary data so accuracy can be improved independently of
the mapcode library.

## Endpoint

```
GET /mapcode/codes/{lat},{lon}/territories
```

- Sits parallel to the existing `/mapcode/codes/{lat},{lon}` family.
- `lat` clamped to `[-90, 90]`, `lon` wrapped to `[-180, 180]` — matches the
  rest of the API.
- JSON by default; XML when the `Accept` header asks for it.
- Async response, matching the pattern of the other endpoints in
  `MapcodeResourceImpl`.

## Response

### Success (HTTP 200)

```json
{
  "territories": [
    { "alphaCode": "USA-CA", "parentAlphaCode": "USA" },
    { "alphaCode": "USA" }
  ]
}
```

- `alphaCode` — mapcode territory code, matching the existing API convention
  (`XXX` for countries, `XX-YY` for subdivisions).
- `parentAlphaCode` — country code when `alphaCode` is a subdivision. Omitted
  from the response when not applicable (matches `@JsonInclude(Include.NON_EMPTY)`
  used elsewhere in the service).
- Order: most specific first. Within the same admin level, smaller polygon area
  first (deterministic tie-breaker for nested or overlapping polygons).

### Empty result (HTTP 200)

```json
{ "territories": [] }
```

Returned when no admin polygon contains the point (sea, Antarctica,
international waters). HTTP 200 — the query was well-formed; the answer is
just "none".

## Ranking

OSM point-in-polygon naturally returns the full set of admin polygons that
contain the point. Ordering rules:

1. **Admin level first** — `admin_level=4` (subdivisions) before
   `admin_level=2` (countries).
2. **Polygon area within level** — smaller area first. Acts as the "most
   specific / most likely de-facto" heuristic for nested or overlapping
   polygons (enclaves, disputed regions).

No editorial layer for disputed territories in v1 — whichever polygon OSM
provides, plus the area tie-breaker, decides the order.

## Output format mapping rules

The endpoint speaks mapcode `alphaCode` (not raw ISO 3166), matching the rest
of the API. The build pipeline (see "Build script" below) applies these rules
when tagging OSM polygons:

- If the OSM polygon's ISO 3166 code maps cleanly to a mapcode `Territory`,
  tag the polygon with the matching `alphaCode`.
- If a subdivision has no mapcode equivalent (mapcode coverage is not 100%
  globally for first-level subdivisions), fall back to the parent country's
  mapcode `Territory` for that polygon — effectively the subdivision is
  represented as "the country" in the response.
- If neither the subdivision nor its parent country has a mapcode equivalent,
  drop the polygon entirely.

## Data source

- **OSM admin-boundary extract**, admin_level 2 (countries) and admin_level 4
  (first-level subdivisions). Matches mapcode's natural granularity.
- **Pre-simplified** with Douglas-Peucker, target tolerance ~50 m. Cuts file
  size and memory footprint by roughly an order of magnitude versus full
  precision with negligible loss for country-level lookups.
- **Format:** FlatGeobuf (`.fgb`). Binary, spatially indexed, streamable,
  purpose-built for in-process point-in-polygon queries.
- **Feature properties:** `alphaCode`, `parentAlphaCode` (optional),
  `adminLevel`, `area` (m²).

## Bundling and distribution

- The borders file is a **sidecar** alongside the service deployment, **not**
  packaged in the JAR.
- A configurable path points the service at the file (new key on
  `MapcodeConfiguration`, e.g., `bordersFilePath`).
- The file is produced once per refresh by the build pipeline, then
  distributed to deployments out-of-band (uploaded to wherever deployments
  fetch from — not the service's concern).

## Build pipeline

Lives in this repo at `tools/build-borders.{sh,py}` (script form, not a
Maven module — lower ceremony, easy to iterate). Pipeline steps:

1. Pull OSM admin_level 2 and 4 boundaries (e.g., via osm-boundaries.com
   extracts or an equivalent source).
2. Read `ISO3166-1:alpha3` (countries) and `ISO3166-2` (subdivisions) tags.
3. Map ISO codes to mapcode `Territory` values. Apply the fallback rule
   from "Output format mapping rules" above.
4. Simplify polygons with Douglas-Peucker, tolerance ~50 m.
5. Compute polygon area for the tie-breaker.
6. Write to FlatGeobuf with feature properties: `alphaCode`,
   `parentAlphaCode`, `adminLevel`, `area`.

The mapcode `Territory` mapping happens once, at build time — the service
itself stays a thin reader. When the mapcode library's territory set changes,
the file is regenerated.

## Service-side components

### Loader

Runs once at startup:

- Reads the configured path.
- **Fails fast** if the file is missing or unreadable — service refuses to
  start. This treats the borders file as a hard runtime dependency. Loud and
  unambiguous, matches the user's preference for surfaced problems over
  silent degradation.
- Loads all features into a JTS `STRtree` indexed by envelope.

### Query path

For each request to the new endpoint:

1. STRtree envelope lookup → candidate features.
2. Refine with JTS `Geometry.contains(point)` against each candidate's
   polygon.
3. Group results by `adminLevel`, sort ascending by `area` within each group.
4. Concatenate: admin_level 4 first, then admin_level 2.
5. Map to DTOs.

### DTOs

New, alongside the existing DTOs in `com.mapcode.services.dto`:

- `TerritoryCandidateDTO`
  - `alphaCode: String` (non-null)
  - `parentAlphaCode: String?` (nullable, omitted via
    `@JsonInclude(Include.NON_EMPTY)`)
- `TerritoryCandidateListDTO`
  - `territories: TerritoryCandidateDTO[]`

Validation, JAX-B, and Swagger annotations follow the patterns of
`TerritoryDTO` / `TerritoryListDTO`.

### Existing endpoints

Unaffected. They continue to use `MapcodeCodec` and the mapcode library's
built-in territory data. The new endpoint is the only OSM-backed route.

### Help text

`RootResourceImpl` documentation (returned by `GET /mapcode`) is updated to
include the new route.

## Dependencies

New runtime dependencies on `service`:

- **FlatGeobuf Java reader** — small library, adds support for reading
  `.fgb` files.
- **JTS Topology Suite** — for `STRtree`, `Geometry.contains`. Verify whether
  this is already pulled in transitively before adding it explicitly.

Build pipeline tooling (Python `geopandas` / equivalent) lives outside the
service classpath in `tools/`.

## Configuration

New configuration key on `MapcodeConfiguration` (exact name to be confirmed
against the existing configuration conventions in this repo). Likely
something like:

- `MapcodeProperties.bordersFilePath` (or equivalent) — absolute path to the
  `.fgb` file.

No reasonable default; the service fails fast if it is absent.

## Tests

In the style of `MapcodeResourceImplTest`:

- **Standard fixture:** a tiny synthetic `borders.fgb` with a handful of
  hand-built polygons covering the test cases below. Generated once, lives
  under `service/src/test/resources/`.
- Point inside a single country (e.g., middle of NL) → `[{NLD}]`.
- Point inside a subdivision (e.g., California) → `[{USA-CA, parent USA},
  {USA}]`.
- Point at sea → `[]`.
- Point in two overlapping admin_level 2 polygons (disputed region) → both,
  smaller-area first.
- Point where the OSM subdivision has no mapcode equivalent → falls back to
  the country.
- Missing borders file at startup → service fails to start (verified via
  whichever pattern existing config-failure tests use).
- JSON and XML response shapes both verified.
- Lat out of range clamped, lon wrapped — matches the behavior of the
  existing endpoints.

## Out of scope (v1)

- **Neighbor candidates near borders.** The `contains` field and "polygons
  within a buffer" idea from earlier brainstorming are deferred. The current
  design returns only territories that geometrically contain the point.
- **Editorial overrides for disputed territories.** No curated priority
  list; the OSM data + area tie-breaker decides.
- **Hot reload of the borders file.** A restart is required to pick up a new
  file.
- **Granularity below admin_level 4** (counties / districts). Mapcode
  territories rarely go that deep, so the data would be largely wasted.

## Future extensions (worth noting, not in v1)

- Optional `?bufferMeters=…` for neighbor candidates within a distance of the
  point — would reintroduce the `contains` field.
- An editorial priority file baked into the build for known disputed regions.
- A `?adminLevel=…` filter for callers that only want countries or only want
  subdivisions.
