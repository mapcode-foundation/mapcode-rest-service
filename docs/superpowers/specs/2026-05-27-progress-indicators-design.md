# Progress Indicators for tools/*.py

**Date:** 2026-05-27  
**Status:** Approved

## Summary

Add stderr progress output to `build-borders.py` so operators can track long runs without third-party dependencies.

## Scope

- **`build-borders.py`** — the only script with a slow execution path (iterates over all OSM polygons).
- **`build-test-borders.py`** — excluded; processes only 7 hardcoded features, trivially fast.

## Design

### Output destination
All progress lines go to **`sys.stderr`**. The existing final summary (`"Wrote N polygons to …"`) stays on **`sys.stdout`** unchanged, so the file stays pipe-friendly.

### What gets annotated
Three slow steps are labelled:

| Step | Message |
|------|---------|
| `gpd.read_file(args.osm)` | `Loading OSM data ...` |
| `gdf.iterrows()` loop start | `Processing {total} polygons ...` |
| Loop body (every 250 rows + final) | `  Processed {i:>{w}}/{total} polygons ({pct:3d}%)` |
| `out_gdf.to_file(...)` | `Writing FlatGeobuf ...` |

### Frequency
Every 250 rows, plus one final line when `i == total`. Width is right-padded to the length of `total` for column alignment.

### Dependencies
None — stdlib only (`sys` is already imported).

## Implementation

~10 lines added to `main()` in `build-borders.py`. No new functions, no new imports.

```python
# before read_file
print("Loading OSM data ...", file=sys.stderr)

gdf = gpd.read_file(args.osm)
gdf = gdf[gdf["admin_level"].isin([2, 4])]

total = len(gdf)
w = len(str(total))
print(f"Processing {total} polygons ...", file=sys.stderr)

rows = []
written = dropped = 0
for i, (_, row) in enumerate(gdf.iterrows(), start=1):  # guarded: skip loop if total == 0
    if i % 250 == 0 or i == total:
        pct = 100 * i // total
        print(f"  Processed {i:{w}}/{total} polygons ({pct:3d}%)", file=sys.stderr)
    # ... rest of loop unchanged ...

print("Writing FlatGeobuf ...", file=sys.stderr)
out_gdf = gpd.GeoDataFrame(rows, crs=gdf.crs)
out_gdf.to_file(args.out, driver="FlatGeobuf")
```

## Acceptance Criteria

- Running `build-borders.py` prints progress to stderr, not stdout.
- A 5000-polygon run produces ~20 progress lines plus the framing messages.
- `python3 -c "import subprocess, sys; r=subprocess.run(['python3','tools/build-borders.py','--help'], capture_output=True); assert not r.stdout.decode().startswith('Processing')"` — stdout untouched.
