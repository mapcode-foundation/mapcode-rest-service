# Memory Footprint Reduction Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the mapcode REST service boot and serve traffic reliably on a 1 GB RAM host by cutting steady-state heap from ~200–400 MB to ≤ 80 MB for boundary data and capping JVM heap to ~70 % of container RAM.

**Architecture:**
1. Cap JVM heap and tune GC via `JAVA_TOOL_OPTIONS` in the Docker image and Procfile.
2. Memory-map `borders.fgb` so the raw bytes live off-heap in the OS page cache.
3. Stop building / priming `PreparedGeometry` for every polygon at startup; build them lazily through a bounded LRU cache instead.
4. Trim the RESTEasy provider list to the JSON/XML/text set actually used by the service.

**Tech Stack:** Java 17, JTS `STRtree` + `PreparedGeometry`, FlatGeobuf, RESTEasy / TJWS, Guice, Maven, JUnit 4.

**Reference spec:** `docs/superpowers/specs/2026-05-28-memory-footprint-reduction-design.md`

---

## File Structure

Files modified by this plan:

| File | Responsibility | Why touched |
|------|---------------|-------------|
| `Dockerfile` | Build + runtime image | Add JVM flags via `ENV JAVA_TOOL_OPTIONS`. |
| `Procfile` | Standalone (non-Docker) launch | Mirror the same JVM flags. |
| `service/src/main/java/com/mapcode/services/implementation/BoundaryService.java` | Loads borders, indexes polygons, answers point-in-polygon queries | Switch to mmap, drop priming, lazy LRU prepared cache. |
| `service/src/test/java/com/mapcode/services/implementation/BoundaryServiceTest.java` | Boundary unit tests | Add coverage for cache behaviour; keep existing tests green. |
| `service/src/main/java/com/mapcode/services/standalone/Server.java` | Embedded server wiring | Remove unused RESTEasy provider registrations. |

No new files. No new Maven dependencies.

---

## Task 1: Pin JVM heap and GC via Docker `JAVA_TOOL_OPTIONS`

**Files:**
- Modify: `Dockerfile`
- Modify: `Procfile`

- [ ] **Step 1: Read the current Dockerfile to confirm structure.**

```bash
cat Dockerfile
```

Expected: a two-stage Dockerfile ending with `CMD ["java", "-jar", "mapcode-rest-service.war"]` and no `ENV JAVA_TOOL_OPTIONS`.

- [ ] **Step 2: Replace the Dockerfile with the same content plus a `JAVA_TOOL_OPTIONS` line.**

Write the entire file (small, safe to overwrite):

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY . .
RUN mvn clean package -pl deployment -am -DskipTests -Dgpg.skip

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /build/deployment/target/mapcode-rest-service.war .

# Memory-footprint flags for small (≤ 1 GB) hosts.
# MaxRAMPercentage caps heap at ~70 % of container RAM, leaving room for native
# (mmap), metaspace, code cache and threads. SerialGC has the smallest native
# overhead on small heaps. ExitOnOutOfMemoryError lets the platform restart the
# container cleanly instead of leaving a wedged JVM.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:InitialRAMPercentage=30 -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=64m -Xss256k -XX:+ExitOnOutOfMemoryError"

CMD ["java", "-jar", "mapcode-rest-service.war"]
```

- [ ] **Step 3: Update Procfile to mirror the same flags.**

Procfile content:

```text
web: java -XX:MaxRAMPercentage=70 -XX:InitialRAMPercentage=30 -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=64m -Xss256k -XX:+ExitOnOutOfMemoryError -jar deployment/target/mapcode-rest-service.war --port $PORT
```

- [ ] **Step 4: Sanity-check the Procfile flags by running the WAR locally with `-Xmx256m` and observing it does not OOM immediately.**

Run (in another shell):

```bash
mvn -pl deployment -am package -DskipTests -Dgpg.skip
java -Xmx256m -XX:+UseSerialGC -jar deployment/target/mapcode-rest-service.war --port 8080
```

Expected: the service either starts and binds to 8080 (good), or fails with `java.lang.OutOfMemoryError: Java heap space` during BoundaryService init (expected today; Task 3 will fix this). Either outcome is fine — this step only verifies the flags are syntactically accepted.

Stop the process with Ctrl-C.

- [ ] **Step 5: Commit.**

```bash
git add Dockerfile Procfile
git commit -m "chore: cap JVM heap and tune GC for small containers"
```

---

## Task 2: Memory-map `borders.fgb` instead of `Files.readAllBytes`

**Goal:** keep the borders bytes off-heap (in the OS page cache).

**Files:**
- Modify: `service/src/main/java/com/mapcode/services/implementation/BoundaryService.java`
- Test: `service/src/test/java/com/mapcode/services/implementation/BoundaryServiceTest.java` (existing tests must stay green; no new test in this task).

- [ ] **Step 1: Run the existing BoundaryService tests to establish a green baseline.**

```bash
mvn -pl service -am test -Dtest=BoundaryServiceTest -q
```

Expected: BUILD SUCCESS, all tests pass.

- [ ] **Step 2: Edit `BoundaryService.java` — replace `Files.readAllBytes` with a `FileChannel.map` call, and replace the `InputStream` byte-array slurp with a temp-file mmap.**

Add these imports (next to the existing `java.nio.*` imports):

```java
import java.io.OutputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
```

Replace the path-based constructor body:

```java
public BoundaryService(@Nonnull final String bordersFilePath) {
    final Path path = Paths.get(bordersFilePath);
    if (!Files.isReadable(path)) {
        throw new IllegalStateException("Borders file not readable: " + path);
    }
    this.index = new STRtree();
    final int loaded;
    try {
        loaded = loadFeatures(mapReadOnly(path));
    } catch (final IOException e) {
        throw new IllegalStateException("Failed to load borders file: " + path, e);
    }
    index.build();
    LOG.info("BoundaryService: loaded {} polygons from {}", loaded, path);
}
```

(Note: the `primePreparedGeometries();` call is **removed** in this step — Task 3 covers the lazy-cache change that makes this safe; for now the field `entries` still exists but is no longer iterated.)

Replace the stream-based constructor body:

```java
public BoundaryService(@Nonnull final InputStream stream,
                       @Nonnull final String sourceDescription) {
    this.index = new STRtree();
    final int loaded;
    final Path tmp;
    try {
        tmp = Files.createTempFile("mapcode-borders-", ".fgb");
        tmp.toFile().deleteOnExit();
        try (final OutputStream out = Files.newOutputStream(tmp, StandardOpenOption.TRUNCATE_EXISTING)) {
            stream.transferTo(out);
        }
        loaded = loadFeatures(mapReadOnly(tmp));
    } catch (final IOException e) {
        throw new IllegalStateException("Failed to load borders from " + sourceDescription, e);
    }
    index.build();
    LOG.info("BoundaryService: loaded {} polygons from {}", loaded, sourceDescription);
}
```

Add the helper method (place it next to the existing private helpers):

```java
@Nonnull
private static MappedByteBuffer mapReadOnly(@Nonnull final Path path) throws IOException {
    try (final FileChannel ch = FileChannel.open(path, StandardOpenOption.READ)) {
        final MappedByteBuffer mapped = ch.map(FileChannel.MapMode.READ_ONLY, 0L, ch.size());
        mapped.order(ByteOrder.LITTLE_ENDIAN);
        return mapped;
    }
}
```

Change `loadFeatures` to take a `ByteBuffer` instead of `byte[]`. Replace the signature line and the very first line of the method:

```java
private int loadFeatures(@Nonnull final ByteBuffer bytes) throws IOException {
    final ByteBuffer buf = bytes.order(ByteOrder.LITTLE_ENDIAN);
    // ... rest of the method unchanged ...
```

Delete the now-unused `toByteArray(InputStream)` helper and its imports (`ByteArrayOutputStream`, `InputStream` if no longer referenced — `InputStream` is still referenced by the constructor signature, so keep it).

- [ ] **Step 3: Compile.**

```bash
mvn -pl service -am compile -q
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Re-run the existing BoundaryService tests.**

```bash
mvn -pl service -am test -Dtest=BoundaryServiceTest -q
```

Expected: BUILD SUCCESS, all tests pass. The behaviour is unchanged — we only swapped how the bytes arrive.

- [ ] **Step 5: Commit.**

```bash
git add service/src/main/java/com/mapcode/services/implementation/BoundaryService.java
git commit -m "perf: memory-map borders.fgb instead of slurping into a byte[]"
```

---

## Task 3: Lazy `PreparedGeometry` via bounded LRU cache

**Goal:** stop building `PreparedGeometry` for every polygon at startup; cache only the recently-used ones.

**Files:**
- Modify: `service/src/main/java/com/mapcode/services/implementation/BoundaryService.java`
- Modify: `service/src/test/java/com/mapcode/services/implementation/BoundaryServiceTest.java`

- [ ] **Step 1: Write a failing test that exercises eviction.**

Append to `BoundaryServiceTest.java`:

```java
    @Test
    public void preparedCacheEvictionDoesNotChangeResults() {
        // Force the prepared cache down to size 1 so every other call evicts.
        // The test fixture has at least 3 distinct polygons (NLD, USA, USA-CA),
        // so alternating lookups must repeatedly evict and rebuild the prepared
        // geometry. Behaviour must remain identical regardless.
        System.setProperty("mapcode.boundary.prepared-cache-size", "1");
        try {
            final BoundaryService svc = new BoundaryService(FIXTURE.toString());
            for (int i = 0; i < 5; i++) {
                assertEquals("NLD", svc.lookup(52.0, 5.0).get(0).getAlphaCode());
                assertEquals("USA-CA", svc.lookup(36.0, -120.0).get(0).getAlphaCode());
                assertTrue(svc.lookup(0.0, -30.0).isEmpty());
            }
        } finally {
            System.clearProperty("mapcode.boundary.prepared-cache-size");
        }
    }
```

- [ ] **Step 2: Run the new test and confirm it fails.**

```bash
mvn -pl service -am test -Dtest=BoundaryServiceTest#preparedCacheEvictionDoesNotChangeResults -q
```

Expected: FAIL. Reason: today every entry holds its own `PreparedGeometry` and there is no cache to size, but the system property is currently unread — the test will pass for the wrong reason (no eviction happens). To make this a real RED, also tighten the test: add an `assertEquals(1, svc.preparedCacheSize());` at the end of the loop. Add the assertion below before re-running:

```java
            // After repeated lookups across many polygons, only one prepared
            // geometry should remain cached.
            assertEquals(1, svc.preparedCacheSize());
```

Re-run:

```bash
mvn -pl service -am test -Dtest=BoundaryServiceTest#preparedCacheEvictionDoesNotChangeResults -q
```

Expected: FAIL with "cannot find symbol: method preparedCacheSize()". Good — now we have a real RED.

- [ ] **Step 3: Edit `BoundaryService.java` — replace eager prepared geometries with a lazy LRU cache.**

Add imports:

```java
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
```

Replace the `IndexedEntry` inner class with one that stores the raw `Geometry`:

```java
    private static final class IndexedEntry {
        @Nonnull final Geometry geometry;
        @Nonnull final String alphaCode;
        @Nullable final String parentAlphaCode;
        final int adminLevel;
        final double area;

        IndexedEntry(
                @Nonnull final Geometry geometry,
                @Nonnull final String alphaCode,
                @Nullable final String parentAlphaCode,
                final int adminLevel,
                final double area) {
            this.geometry = geometry;
            this.alphaCode = alphaCode;
            this.parentAlphaCode = parentAlphaCode;
            this.adminLevel = adminLevel;
            this.area = area;
        }
    }
```

Remove the `entries` field, the `PRIMING_POINT` constant and the `primePreparedGeometries()` method (no longer needed). Remove the imports for `PreparedGeometryFactory` only if no longer referenced — it is still used by the cache loader below, so keep it.

Add the cache fields and a configurable size, immediately under the existing `index` field:

```java
    private static final int DEFAULT_PREPARED_CACHE_SIZE = 200;
    private final int preparedCacheSize = Integer.parseInt(
            System.getProperty("mapcode.boundary.prepared-cache-size",
                    String.valueOf(DEFAULT_PREPARED_CACHE_SIZE)));

    /** Access-ordered LRU. Keys are identity of IndexedEntry (geometry is unique per entry). */
    private final Map<IndexedEntry, PreparedGeometry> preparedCache =
            Collections.synchronizedMap(new LinkedHashMap<IndexedEntry, PreparedGeometry>(
                    16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(
                        final Map.Entry<IndexedEntry, PreparedGeometry> eldest) {
                    return size() > preparedCacheSize;
                }
            });
```

In `loadFeatures`, replace the lines that built a `PreparedGeometry` and constructed `IndexedEntry` with the new form. Specifically, replace:

```java
            final PreparedGeometry prepared = PreparedGeometryFactory.prepare(geometry);
            final IndexedEntry entry = new IndexedEntry(prepared, alphaCode, parentAlphaCode,
                    adminLevel, area);
            index.insert(geometry.getEnvelopeInternal(), entry);
            entries.add(entry);
            count++;
```

with:

```java
            final IndexedEntry entry = new IndexedEntry(geometry, alphaCode, parentAlphaCode,
                    adminLevel, area);
            index.insert(geometry.getEnvelopeInternal(), entry);
            count++;
```

Rewrite `lookup` to use the cache:

```java
    @Nonnull
    public List<TerritoryMatch> lookup(final double latDeg, final double lonDeg) {
        final Coordinate coord = new Coordinate(lonDeg, latDeg);
        final Envelope env = new Envelope(coord);
        @SuppressWarnings("unchecked")
        final List<IndexedEntry> candidates = index.query(env);
        final Point point = GEOMETRY_FACTORY.createPoint(coord);
        final List<TerritoryMatch> hits = new ArrayList<>(candidates.size());
        for (final IndexedEntry e : candidates) {
            final PreparedGeometry prepared =
                    preparedCache.computeIfAbsent(e,
                            k -> PreparedGeometryFactory.prepare(k.geometry));
            if (prepared.contains(point)) {
                hits.add(new TerritoryMatch(e.alphaCode, e.parentAlphaCode, e.adminLevel, e.area));
            }
        }
        hits.sort(Comparator
                .comparingInt(TerritoryMatch::getAdminLevel).reversed()
                .thenComparingDouble(TerritoryMatch::getArea));
        return hits;
    }
```

Expose cache size for tests (package-private is enough; place it just above the inner `IndexedEntry` class):

```java
    /** Visible for testing — current number of cached prepared geometries. */
    int preparedCacheSize() {
        synchronized (preparedCache) {
            return preparedCache.size();
        }
    }
```

- [ ] **Step 4: Compile.**

```bash
mvn -pl service -am compile -q
```

Expected: BUILD SUCCESS. If `entries` or `primePreparedGeometries` references remain, delete them.

- [ ] **Step 5: Run the new eviction test.**

```bash
mvn -pl service -am test -Dtest=BoundaryServiceTest#preparedCacheEvictionDoesNotChangeResults -q
```

Expected: PASS.

- [ ] **Step 6: Run the full BoundaryServiceTest suite to verify no regressions.**

```bash
mvn -pl service -am test -Dtest=BoundaryServiceTest -q
```

Expected: BUILD SUCCESS, all six tests pass.

- [ ] **Step 7: Run the whole service module test suite.**

```bash
mvn -pl service -am test -q
```

Expected: BUILD SUCCESS.

- [ ] **Step 8: Commit.**

```bash
git add service/src/main/java/com/mapcode/services/implementation/BoundaryService.java \
        service/src/test/java/com/mapcode/services/implementation/BoundaryServiceTest.java
git commit -m "perf: build PreparedGeometry lazily via bounded LRU cache"
```

---

## Task 4: Trim unused RESTEasy providers

**Goal:** stop registering ~30 providers the service does not use (multipart, IIOImage, XOP, form, source, document, etc.).

**Files:**
- Modify: `service/src/main/java/com/mapcode/services/standalone/Server.java`

- [ ] **Step 1: Replace the provider-registration block in `Server.java` lines 122–168 with a slim list.**

Find this block in `startServer`:

```java
        providerFactory.registerProvider(JAXBXmlSeeAlsoProvider.class, true);
        // ... ~35 lines ...
        providerFactory.registerProvider(XopWithMultipartRelatedWriter.class, true);
```

Replace it with:

```java
        // Slim provider set: JSON (Jackson), XML (JAXB), plain text.
        // The mapcode service has no multipart, form, file, image, or XOP endpoints.
        providerFactory.registerProvider(JAXBXmlSeeAlsoProvider.class, true);
        providerFactory.registerProvider(JAXBXmlRootElementProvider.class, true);
        providerFactory.registerProvider(JAXBElementProvider.class, true);
        providerFactory.registerProvider(JAXBXmlTypeProvider.class, true);
        providerFactory.registerProvider(XmlJAXBContextFinder.class, true);
        providerFactory.registerProvider(DefaultTextPlain.class, true);
        providerFactory.registerProvider(StringTextStar.class, true);
        providerFactory.registerProvider(CacheControlFeature.class, true);
        providerFactory.registerProvider(ResteasyJackson2Provider.class, true);
```

- [ ] **Step 2: Clean up the now-unused imports at the top of `Server.java`.**

Remove these import lines (or any single-class entries; if the source uses wildcard `import ... .multipart.*;` and `... .providers.*;` then leave the wildcards — IDE will not complain):

```java
import org.jboss.resteasy.plugins.providers.multipart.*;
```

After editing, verify the file still has these imports (they are still needed):

```java
import org.jboss.resteasy.plugins.interceptors.CacheControlFeature;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;
import org.jboss.resteasy.plugins.providers.jaxb.*;
import org.jboss.resteasy.plugins.providers.DefaultTextPlain;
import org.jboss.resteasy.plugins.providers.StringTextStar;
```

(The wildcard `org.jboss.resteasy.plugins.providers.*` previously covered both `DefaultTextPlain` and `StringTextStar`; if you keep that wildcard, you do not need the explicit single-class imports.)

- [ ] **Step 3: Compile.**

```bash
mvn -pl service -am compile -q
```

Expected: BUILD SUCCESS, no "cannot find symbol" errors for the removed providers.

- [ ] **Step 4: Run the API integration tests (these exercise real JSON+XML serialisation through the embedded server).**

```bash
mvn -pl service -am test -q
```

Expected: BUILD SUCCESS, every `Api*Test` class green. If any test fails because a JSON/XML response is no longer produced, restore the specific provider it needs (don't restore the whole list).

- [ ] **Step 5: Commit.**

```bash
git add service/src/main/java/com/mapcode/services/standalone/Server.java
git commit -m "chore: register only the RESTEasy providers the service actually uses"
```

---

## Task 5: Verify memory budget under a 1 GB container

**Goal:** confirm the combined effect of tasks 1–4 fits inside a 1 GB container with headroom.

**Files:** none modified.

- [ ] **Step 1: Build the WAR.**

```bash
mvn -pl deployment -am package -DskipTests -Dgpg.skip -q
```

Expected: `deployment/target/mapcode-rest-service.war` exists.

- [ ] **Step 2: Build the Docker image.**

```bash
docker build -t mapcode-rest-service:mem-test .
```

Expected: image builds; `docker images mapcode-rest-service:mem-test` shows the new image.

- [ ] **Step 3: Run the image with a hard 1 GB memory cap and a probe port.**

```bash
docker run --rm --memory=1g --memory-swap=1g -p 8080:8080 \
    --name mapcode-mem-test mapcode-rest-service:mem-test
```

Expected: log line `BoundaryService: loaded N polygons from classpath:/borders.fgb` followed by `Server: server is ready`. The container should NOT be killed with exit code 137 (OOM-killed). Leave it running for step 4.

- [ ] **Step 4: Drive a small workload and inspect heap.**

In another shell:

```bash
for i in $(seq 1 200); do
  curl -s "http://localhost:8080/mapcode/codes/$((RANDOM % 180 - 90)).0,$((RANDOM % 360 - 180)).0" > /dev/null
done
docker exec mapcode-mem-test jcmd 1 GC.heap_info
docker stats --no-stream mapcode-mem-test
```

Expected:
- `GC.heap_info` shows `used` well below 350 MB.
- `docker stats` shows total RSS (`MEM USAGE`) below ~600 MB.
- No `OutOfMemoryError` in the container logs.

- [ ] **Step 5: Stop the container.**

```bash
docker stop mapcode-mem-test
```

- [ ] **Step 6: No code change — no commit. Record the observed `GC.heap_info` numbers in the spec's "Rollout" section if you want a record.**

---

## Final verification

- [ ] **Step 1: Confirm `git status` is clean.**

```bash
git status
```

Expected: `nothing to commit, working tree clean`.

- [ ] **Step 2: Run the full project test suite once more.**

```bash
mvn -pl service,deployment -am test -q
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Push.**

```bash
git push
```

Expected: push succeeds; `git status` shows `up to date with origin`.
