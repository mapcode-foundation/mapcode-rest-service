# Memory Footprint Reduction — Design

**Date:** 2026-05-28
**Status:** Draft, awaiting review
**Target:** run reliably on a 1 GB RAM host (e.g. DigitalOcean `basic-xs`).

## Problem

The mapcode REST service intermittently runs out of heap on 1 GB RAM machines.
No `-Xmx` is configured anywhere (`Dockerfile`, `Procfile`, `.do/`), so the JVM
picks an ergonomic default that on small containers can easily collide with the
sum of resident set + native + metaspace and trigger the kernel OOM killer or
`OutOfMemoryError`.

The dominant heap consumer is the boundary lookup data: `borders.fgb` (the
22 MB bundled file) is fully read into a `byte[]`, every polygon is deserialized
into a JTS `Geometry`, wrapped in a `PreparedGeometry`, and **all prepared
geometries are primed at startup** (segment-tree indexes built and held forever).
For an OSM-admin-boundary planet file this typically inflates to **~200–400 MB of
live heap** that never shrinks.

Other contributors:
- Bundled FGB on the classpath is consumed via `ByteArrayOutputStream`, roughly
  doubling peak load memory.
- `Main.java` extracts every nested JAR from the WAR to a temp dir at startup
  (disk only, but adds GC churn and load latency).
- RESTEasy registers ~35 providers, many of which are unused (multipart, IIOImage,
  XOP, several JAXB variants).
- `STRtree` holds the full polygon set live forever.

## Goals

1. Service starts and serves traffic on a 1 GB host with comfortable headroom
   (target: stable steady-state heap ≤ 350 MB, peak ≤ 500 MB).
2. No loss of correctness for `/codes` and `/coords` endpoints.
3. Acceptable: small cold-path latency increase on `/territories` and
   territory-aware `/codes` (first hits in a region pay a one-time cost).
4. No new operational dependencies — must keep working with the bundled FGB.

## Non-Goals

- Reducing the on-disk size of `borders.fgb` (already done in commit
  `f6380ec smaller boundary file`).
- Restructuring or replacing the REST framework, DI container, or JTS.
- Sub-100 ms first-request latency.

## Design

The plan has four work packages, ordered by ROI and increasing risk.
Packages 1–3 should be implemented together; package 4 is optional and only if
the others are not enough on their own.

### Package 1 — JVM & container tuning (low risk, large win)

Add explicit JVM flags to `Dockerfile` and `Procfile`. The goal is to prevent
the JVM from sizing heap as if the host had unlimited RAM and to use a memory-
frugal GC.

Concrete flags (Java 17, JRE image):

```text
-XX:MaxRAMPercentage=70
-XX:InitialRAMPercentage=30
-XX:+UseSerialGC
-XX:MaxMetaspaceSize=128m
-XX:ReservedCodeCacheSize=64m
-Xss256k
-XX:+ExitOnOutOfMemoryError
```

Rationale:
- `MaxRAMPercentage=70` on a 1 GB host caps heap at ~700 MB, leaving headroom
  for native (mmap), metaspace, code cache, threads.
- `UseSerialGC` has the smallest native overhead and lowest steady-state RSS for
  small heaps; G1 is fine but its remembered sets cost ~5 % of heap.
- `MaxMetaspaceSize` and `ReservedCodeCacheSize` bound JIT / class metadata
  (RESTEasy + Jackson + JAXB load a lot of classes).
- `Xss256k` cuts per-thread stack reservation; with TJWS spawning a worker
  thread per request this matters.
- `ExitOnOutOfMemoryError` lets the orchestrator restart cleanly rather than
  thrashing.

`Dockerfile` change: pass these via `JAVA_TOOL_OPTIONS` so they apply to both
`CMD` and Procfile launches.

### Package 2 — Slim the BoundaryService load path (medium risk, large win)

Three small, independent changes inside `BoundaryService`:

**2a. Memory-map the FGB instead of `readAllBytes`.**
Replace `Files.readAllBytes(path)` with `FileChannel.map(READ_ONLY, 0, size)`
returning a `MappedByteBuffer`. For the classpath/`InputStream` case, extract
the resource once to a temp file at startup (`Files.copy(stream, tmp)`) and
mmap that. Effect: borders bytes live in OS page cache (off-heap, swappable),
not in the Java heap. Saves 22 MB of heap during load and forever after; the
JVM only sees a `ByteBuffer` view.

**2b. Drop the priming step.**
`primePreparedGeometries()` walks every entry and calls
`prepared.contains(PRIMING_POINT)` to force the lazy segment-tree index to be
built at startup. This is the single largest avoidable allocation: prepared
indexes for every polygon are pinned in heap permanently, even for polygons
that no request will ever touch. Remove the priming call; let
`PreparedGeometry` build its index lazily on first contains-check.

**2c. Replace `PreparedGeometry` with a sized LRU cache keyed by polygon id.**
Most lookups concentrate on a handful of high-traffic territories. Store the
raw `Geometry` in `IndexedEntry` (cheap), and lazily build / cache its
`PreparedGeometry` in an LRU sized to, say, 200 entries (tunable via
`mapcode.boundary.prepared-cache-size`). Cache eviction reclaims segment-tree
memory. Implementation: `LinkedHashMap` subclass with
`removeEldestEntry(...)` or Caffeine if already on the classpath; otherwise
LinkedHashMap to avoid a new dependency.

After 2a + 2b + 2c, expected steady-state boundary heap drops from 200–400 MB
to roughly **30–80 MB** (raw `Geometry` + `STRtree` + bounded prepared cache).

### Package 3 — Trim RESTEasy providers (low risk, small-but-free win)

`Server.java` lines 130–167 register ~35 providers, of which the service
genuinely uses only JSON, XML (JAXB), and plain text. Remove the unused
families:

- All multipart providers (12 classes) — service has no multipart endpoints.
- `IIOImageProvider`, `DataSourceProvider`, `DocumentProvider`,
  `SourceProvider`, `FileProvider`, `FileRangeWriter`, `StreamingOutputProvider`,
  `XopWith*` — none are used by `MapcodeResource` / `RootResource`.
- `JaxrsFormProvider`, `FormUrlEncodedProvider` — no form endpoints.

Each provider drags in `MessageBodyReader`/`Writer` instances and helper state.
Expected saving is modest (a few MB heap + faster startup + smaller metaspace),
but the change is trivial and reduces attack surface.

### Package 4 — Optional: stop extracting nested JARs to disk (medium risk)

`Main.java` walks the WAR and copies every nested JAR to a temp directory at
startup. This is a workaround for the deprecated `nestedjar://` scheme but it
roughly doubles startup I/O and leaves ~50 MB of files in `/tmp` that must be
GC'd later. Two options:

- **4a (recommended if package 1+2 already meets the budget):** leave it. It is
  disk, not heap, so it does not contribute to OOM.
- **4b:** switch deployment to a Spring Boot or Maven Shade fat-jar layout,
  removing the nested-jar dance entirely. This is a bigger refactor and out of
  scope for the OOM fix; track as a separate follow-up if startup time becomes
  an issue.

## Data Flow Changes

Before:
```
Startup:  borders.fgb -> readAllBytes -> ByteBuffer -> decode all Geometry
                                                    -> PreparedGeometryFactory.prepare for each
                                                    -> contains(PRIMING_POINT) for each   ← pins segment trees
                                                    -> STRtree.insert(envelope, IndexedEntry{prepared, ...})

Request:  STRtree.query(env) -> for each candidate: entry.prepared.contains(point)
```

After:
```
Startup:  borders.fgb -> FileChannel.map -> MappedByteBuffer (off-heap)
                                         -> decode all Geometry
                                         -> STRtree.insert(envelope, IndexedEntry{geometry, ...})

Request:  STRtree.query(env) -> for each candidate:
              prepared = preparedCache.computeIfAbsent(entry.id,
                            id -> PreparedGeometryFactory.prepare(entry.geometry))
              prepared.contains(point)
              (LRU evicts least-recently-used prepared geometries when cache is full)
```

## Components

| File | Change |
|------|--------|
| `Dockerfile` | Add `ENV JAVA_TOOL_OPTIONS=...` with flags from package 1. |
| `Procfile` | Same flags (kept in sync). |
| `BoundaryService.java` | Mmap load, drop priming, store `Geometry` not `PreparedGeometry`, add LRU prepared cache, expose cache-size sys-property. |
| `Server.java` | Remove unused RESTEasy provider registrations (package 3). |
| `service/src/test/.../BoundaryServiceTest.java` | Update tests that asserted on priming behavior; add a test that hammers a single point repeatedly and asserts the prepared cache is populated. |

## Error Handling

- Memory-mapping a missing or unreadable file: fall back to existing
  `IllegalStateException` path (same message as today).
- LRU cache: use a `LinkedHashMap` with `accessOrder=true` and an overridden
  `removeEldestEntry`, wrapped in `Collections.synchronizedMap`. Lookup volume
  is modest and almost all calls hit the cache, so a single coarse lock is
  acceptable; revisit only if profiling shows contention.
- JVM OOM: `ExitOnOutOfMemoryError` ensures clean restart rather than a half-
  dead process; the platform restarts the container.

## Testing

1. **Unit tests:** keep existing `BoundaryServiceTest`; add coverage for
   - lookup correctness when prepared cache is cold,
   - lookup correctness after cache eviction (force size=2, query 4 distinct
     points),
   - that no `PreparedGeometry` is built at startup (count via a debug counter
     or just measure load time).
2. **Local heap test:** start with `-Xmx256m` and run `LocalTestServer`;
   exercise `/codes` and `/territories` for ~10 k random points. Today this
   should OOM; after the change it should pass.
3. **Container test:** `docker run --memory=1g` the produced image and run the
   same load. Inspect `jcmd <pid> GC.heap_info` and `VM.native_memory` (if
   NMT enabled in a debug build) to confirm steady-state numbers.
4. **Regression test:** baseline `/codes/{lat,lon}` latency on warm cache; new
   path should be within ±10 % for warm requests, with cold first-region hits
   allowed to be ~5–20 ms slower.

## Risks

- **LRU thrash on adversarial traffic** (queries that always miss the cache).
  Mitigation: cache size is configurable; default 200 is well above realistic
  hot-set; worst case is repeated rebuild of one prepared geometry per query —
  same cost as the pre-PreparedGeometry implementation we used before.
- **Mmap on read-only filesystems / Heroku-style ephemeral containers:** the
  classpath path still extracts to a temp file first, so behavior is
  equivalent.
- **`ExitOnOutOfMemoryError`** means a single bad request that allocates wildly
  takes the process down. We do not currently have such endpoints, and the
  alternative (a wedged JVM) is worse.

## Out of Scope

- Replacing JTS with a smaller geometry library.
- Re-encoding `borders.fgb` to a custom column-oriented format.
- Removing or replacing TJWS.
- Spring Boot / fat-jar migration (package 4b).

## Rollout

1. Land packages 1, 2, 3 in a single PR (small enough; tightly coupled by their
   shared goal of "boots and serves on 1 GB").
2. Verify locally with `docker run --memory=1g`.
3. Deploy to the basic-xs droplet; watch RSS over 24 h.
4. Tune `mapcode.boundary.prepared-cache-size` only if observed cache hit-rate
   is poor.
