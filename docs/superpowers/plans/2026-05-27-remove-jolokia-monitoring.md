# Remove Jolokia Monitoring Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Completely remove Jolokia, the JMX metrics layer, the `/mapcode/metrics` REST endpoint, and the example monitoring client from mapcode-rest-service.

**Architecture:** Six sequential commits, each leaving the project in a compilable and test-passing state. Removes in dependency order: external config first (Jolokia), then call sites (MapcodeResourceImpl, RootResourceImpl), then Guice wiring, then the now-unreferenced class files, then docs.

**Tech Stack:** Java 8, Maven multi-module, Guice DI, JAX-RS/RESTEasy, JUnit 4

---

## Files Changed

| File | Action |
|------|--------|
| `deployment/src/main/webapp/WEB-INF/web.xml` | Remove JolokiaAgent servlet + mapping |
| `pom.xml` | Remove `jolokia.version` property + `jolokia-core` dep block |
| `deployment/pom.xml` | Remove `jolokia-core` dep reference |
| `example/monitorclient/` | Delete entire directory |
| `service/src/main/java/com/mapcode/services/implementation/MapcodeResourceImpl.java` | Remove `SystemMetricsCollector` field, constructor param, and all 10 call sites |
| `service/src/main/java/com/mapcode/services/standalone/Server.java` | Remove `SystemMetricsImpl` construction + pass to `MapcodeResourceImpl`, then `RootResourceImpl` |
| `service/src/test/java/com/mapcode/services/LocalTestServer.java` | Same as Server.java |
| `service/src/main/java/com/mapcode/services/RootResource.java` | Remove `getMetrics(...)` method |
| `service/src/main/java/com/mapcode/services/implementation/RootResourceImpl.java` | Remove `SystemMetrics` field, constructor param, `getMetrics(...)` impl, HELP_TEXT references |
| `service/src/test/java/com/mapcode/services/ApiOthersTest.java` | Remove `checkMetrics()` test |
| `service/src/main/java/com/mapcode/services/ResourcesModule.java` | Remove SystemMetrics* imports, bindings, and provider methods |
| `service/src/main/java/com/mapcode/services/metrics/SystemMetrics.java` | Delete |
| `service/src/main/java/com/mapcode/services/metrics/SystemMetricsCollector.java` | Delete |
| `service/src/main/java/com/mapcode/services/implementation/SystemMetricsImpl.java` | Delete |
| `service/src/main/java/com/mapcode/services/jmx/SystemMetricsAgent.java` | Delete |
| `README.md` | Remove `/mapcode/metrics` references |

---

## Task 1: Remove Jolokia servlet, dependency, and example client

**Files:**
- Modify: `deployment/src/main/webapp/WEB-INF/web.xml`
- Modify: `pom.xml`
- Modify: `deployment/pom.xml`
- Delete: `example/monitorclient/` (entire directory)

- [ ] **Step 1: Remove JolokiaAgent servlet from web.xml**

In `deployment/src/main/webapp/WEB-INF/web.xml`, remove this block:

```xml
    <servlet>
        <servlet-name>JolokiaAgent</servlet-name>
        <servlet-class>org.jolokia.http.AgentServlet</servlet-class>
        <load-on-startup>1</load-on-startup>
    </servlet>

    <servlet-mapping>
        <servlet-name>JolokiaAgent</servlet-name>
        <url-pattern>/monitor/*</url-pattern>
    </servlet-mapping>
```

- [ ] **Step 2: Verify no Jolokia references in web.xml**

```bash
grep -i "jolokia\|monitor" deployment/src/main/webapp/WEB-INF/web.xml
```
Expected: no output.

- [ ] **Step 3: Remove jolokia.version property from root pom.xml**

In `pom.xml`, remove this line (around line 104):

```xml
        <jolokia.version>1.7.1</jolokia.version>
```

- [ ] **Step 4: Remove jolokia-core dependency block from root pom.xml**

In `pom.xml`, remove this block (around lines 394–398):

```xml
            <dependency>
                <groupId>org.jolokia</groupId>
                <artifactId>jolokia-core</artifactId>
                <version>${jolokia.version}</version>
            </dependency>
```

- [ ] **Step 5: Remove jolokia-core dependency from deployment/pom.xml**

In `deployment/pom.xml`, remove this block (around lines 94–97):

```xml
        <dependency>
            <groupId>org.jolokia</groupId>
            <artifactId>jolokia-core</artifactId>
        </dependency>
```

- [ ] **Step 6: Delete the example monitoring client directory**

```bash
git rm -r example/monitorclient/
```

- [ ] **Step 7: Verify no Jolokia references remain**

```bash
grep -ri "jolokia" pom.xml deployment/pom.xml deployment/src/main/webapp/WEB-INF/web.xml
```
Expected: no output.

- [ ] **Step 8: Verify build passes**

```bash
mvn compile -pl service -am -q 2>&1 | tail -3
```
Expected: `BUILD SUCCESS`.

- [ ] **Step 9: Commit**

```bash
git add deployment/src/main/webapp/WEB-INF/web.xml pom.xml deployment/pom.xml
git commit -m "chore: remove Jolokia servlet, dependency, and example client

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 2: Remove metrics collection from MapcodeResourceImpl

**Files:**
- Modify: `service/src/main/java/com/mapcode/services/implementation/MapcodeResourceImpl.java`
- Modify: `service/src/main/java/com/mapcode/services/standalone/Server.java`
- Modify: `service/src/test/java/com/mapcode/services/LocalTestServer.java`

- [ ] **Step 1: Remove SystemMetricsCollector import from MapcodeResourceImpl**

In `MapcodeResourceImpl.java`, remove line 26:

```java
import com.mapcode.services.metrics.SystemMetricsCollector;
```

- [ ] **Step 2: Remove SystemMetricsCollector field from MapcodeResourceImpl**

Remove line 61:

```java
    private final SystemMetricsCollector metricsCollector;
```

- [ ] **Step 3: Remove metricsCollector constructor parameter and body lines from MapcodeResourceImpl**

The constructor currently looks like:

```java
    @Inject
    public MapcodeResourceImpl(
            @Nonnull final ResourceProcessor processor,
            @Nonnull final SystemMetricsCollector metricsCollector,
            @Nonnull final BoundaryService boundaryService) {
        assert processor != null;
        assert metricsCollector != null;
        assert boundaryService != null;
        this.processor = processor;
        this.metricsCollector = metricsCollector;
        this.boundaryService = boundaryService;
    }
```

Change it to:

```java
    @Inject
    public MapcodeResourceImpl(
            @Nonnull final ResourceProcessor processor,
            @Nonnull final BoundaryService boundaryService) {
        assert processor != null;
        assert boundaryService != null;
        this.processor = processor;
        this.boundaryService = boundaryService;
    }
```

Also remove the Javadoc line `* @param metricsCollector Metric collector.` from the constructor comment above it.

- [ ] **Step 4: Remove all metricsCollector call sites in MapcodeResourceImpl**

```bash
grep -n "metricsCollector" service/src/main/java/com/mapcode/services/implementation/MapcodeResourceImpl.java
```

Remove every line that calls `metricsCollector.addOne...()`. There are 10 such lines at approximately: 152, 409, 433, 462, 496, 560, 579, 615, 658, 694. Each is a single standalone statement — delete the line (and blank line above it if it was used for spacing, as appropriate).

- [ ] **Step 5: Update Server.java — remove SystemMetricsImpl construction and pass to MapcodeResourceImpl**

In `service/src/main/java/com/mapcode/services/standalone/Server.java`:

Remove line 79:
```java
        final SystemMetricsImpl metrics = new SystemMetricsImpl();
```

Change the `MapcodeResourceImpl` constructor call (around line 93):

Before:
```java
        final MapcodeResourceImpl mapcodeResource = new MapcodeResourceImpl(
                resourceProcessor,
                metrics,
                boundaryService
        );
```

After:
```java
        final MapcodeResourceImpl mapcodeResource = new MapcodeResourceImpl(
                resourceProcessor,
                boundaryService
        );
```

**Note:** Leave the `metrics` variable passed to `RootResourceImpl` for now — that changes in Task 3. The file will not compile yet unless you temporarily introduce a separate `metrics` variable. Instead, leave `metrics` where it is and re-add it as just the RootResource argument — i.e., replace the deleted line with:

```java
        final SystemMetricsImpl metrics = new SystemMetricsImpl();
```

...and keep it for `RootResourceImpl`. (The full cleanup of `metrics` from `Server.java` happens in Task 3.)

- [ ] **Step 6: Update LocalTestServer.java — remove metrics pass to MapcodeResourceImpl**

In `service/src/test/java/com/mapcode/services/LocalTestServer.java`:

Change the `MapcodeResourceImpl` constructor call (around line 82):

Before:
```java
        final MapcodeResourceImpl mapcodeResource = new MapcodeResourceImpl(
                resourceProcessor,
                metrics,
                boundaryService
        );
```

After:
```java
        final MapcodeResourceImpl mapcodeResource = new MapcodeResourceImpl(
                resourceProcessor,
                boundaryService
        );
```

Leave the `metrics` variable and its use in `RootResourceImpl` — those change in Task 3.

- [ ] **Step 7: Verify no metricsCollector references remain in MapcodeResourceImpl**

```bash
grep -n "metricsCollector\|SystemMetricsCollector" \
  service/src/main/java/com/mapcode/services/implementation/MapcodeResourceImpl.java
```
Expected: no output.

- [ ] **Step 8: Verify build and tests pass**

```bash
mvn test -pl service -am -q 2>&1 | grep -E "BUILD|Tests run.*FAILURE|ERROR" | tail -5
```
Expected: `BUILD SUCCESS`, no failures.

- [ ] **Step 9: Commit**

```bash
git add service/src/main/java/com/mapcode/services/implementation/MapcodeResourceImpl.java \
        service/src/main/java/com/mapcode/services/standalone/Server.java \
        service/src/test/java/com/mapcode/services/LocalTestServer.java
git commit -m "refactor: remove metrics collection from MapcodeResourceImpl

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 3: Remove /mapcode/metrics REST endpoint

**Files:**
- Modify: `service/src/main/java/com/mapcode/services/RootResource.java`
- Modify: `service/src/main/java/com/mapcode/services/implementation/RootResourceImpl.java`
- Modify: `service/src/main/java/com/mapcode/services/standalone/Server.java`
- Modify: `service/src/test/java/com/mapcode/services/LocalTestServer.java`
- Modify: `service/src/test/java/com/mapcode/services/ApiOthersTest.java`

- [ ] **Step 1: Remove getMetrics from RootResource interface**

In `service/src/main/java/com/mapcode/services/RootResource.java`, remove:

```java
    /**
     * This method returns system metrics.
     *
     * @param response Returns a system metrics.
     */
    @Path("metrics")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    void getMetrics(@Suspended @Nonnull AsyncResponse response);
```

- [ ] **Step 2: Remove SystemMetrics import from RootResourceImpl**

In `RootResourceImpl.java`, remove line 24:

```java
import com.mapcode.services.metrics.SystemMetrics;
```

- [ ] **Step 3: Remove metrics field and constructor parameter from RootResourceImpl**

The field (line 194):
```java
    private final SystemMetrics metrics;
```

The constructor currently:
```java
    @Inject
    public RootResourceImpl(
            @Nonnull final MapcodeResource mapcodeResource,
            @Nonnull final MavenProperties mavenProperties,
            @Nonnull final SystemMetrics metrics) {
        assert mapcodeResource != null;
        assert mavenProperties != null;
        assert metrics != null;

        // Store the injected values.
        this.mapcodeResource = mapcodeResource;
        this.mavenProperties = mavenProperties;
        this.metrics = metrics;
    }
```

Change to:
```java
    @Inject
    public RootResourceImpl(
            @Nonnull final MapcodeResource mapcodeResource,
            @Nonnull final MavenProperties mavenProperties) {
        assert mapcodeResource != null;
        assert mavenProperties != null;

        // Store the injected values.
        this.mapcodeResource = mapcodeResource;
        this.mavenProperties = mavenProperties;
    }
```

- [ ] **Step 4: Remove getMetrics implementation from RootResourceImpl**

Remove the entire method:

```java
    @Override
    public void getMetrics(@Suspended @Nonnull final AsyncResponse response) {
        assert response != null;
        LOG.info("getMetrics");

        // No input validation required. Just return metrics as a plain JSON string.
        final String json = Json.toJson(metrics);
        response.resume(Response.ok(json).build());
    }
```

- [ ] **Step 5: Remove metrics references from HELP_TEXT in RootResourceImpl**

In the `HELP_TEXT` static field, remove these two lines:

```java
            "All REST services (except 'metrics') are able to return both JSON and XML. Use the HTTP\n" +
```
→ replace with:
```java
            "All REST services are able to return both JSON and XML. Use the HTTP\n" +
```

And remove this line entirely:
```java
            "GET /mapcode/metrics Returns some system metrics (JSON-only, also available from JMX).\n" +
```

- [ ] **Step 6: Update Server.java — remove SystemMetricsImpl and metrics from RootResourceImpl**

In `service/src/main/java/com/mapcode/services/standalone/Server.java`:

Remove the now-leftover line:
```java
        final SystemMetricsImpl metrics = new SystemMetricsImpl();
```

Change `RootResourceImpl` constructor call:

Before:
```java
        final RootResourceImpl rootResource = new RootResourceImpl(
                mapcodeResource,
                mavenProperties,
                metrics
        );
```

After:
```java
        final RootResourceImpl rootResource = new RootResourceImpl(
                mapcodeResource,
                mavenProperties
        );
```

- [ ] **Step 7: Update LocalTestServer.java — remove metrics from RootResourceImpl**

In `service/src/test/java/com/mapcode/services/LocalTestServer.java`:

Remove the `metrics` variable:
```java
        final SystemMetricsImpl metrics = new SystemMetricsImpl();
```

Change `RootResourceImpl` constructor call:

Before:
```java
        final RootResourceImpl rootResource = new RootResourceImpl(
                mapcodeResource,
                mavenProperties,
                metrics
        );
```

After:
```java
        final RootResourceImpl rootResource = new RootResourceImpl(
                mapcodeResource,
                mavenProperties
        );
```

- [ ] **Step 8: Remove checkMetrics test from ApiOthersTest.java**

In `service/src/test/java/com/mapcode/services/ApiOthersTest.java`, run:

```bash
grep -n "checkMetrics\|getMetrics\|mapcode/metrics" \
  service/src/test/java/com/mapcode/services/ApiOthersTest.java
```

Remove the entire `checkMetrics()` method — from the `@Test` annotation through the closing `}`. The method starts at the `@Test` above `public void checkMetrics()` and ends at the `}` after the two `Assert.assertEquals(...)` calls (approximately lines 141–155). Delete those lines entirely.

- [ ] **Step 9: Verify build and tests pass**

```bash
mvn test -pl service -am -q 2>&1 | grep -E "BUILD|Tests run.*FAILURE|ERROR" | tail -5
```
Expected: `BUILD SUCCESS`, no failures.

- [ ] **Step 10: Commit**

```bash
git add service/src/main/java/com/mapcode/services/RootResource.java \
        service/src/main/java/com/mapcode/services/implementation/RootResourceImpl.java \
        service/src/main/java/com/mapcode/services/standalone/Server.java \
        service/src/test/java/com/mapcode/services/LocalTestServer.java \
        service/src/test/java/com/mapcode/services/ApiOthersTest.java
git commit -m "refactor: remove /mapcode/metrics REST endpoint

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 4: Remove metrics Guice wiring

**Files:**
- Modify: `service/src/main/java/com/mapcode/services/ResourcesModule.java`

- [ ] **Step 1: Remove SystemMetrics* imports from ResourcesModule**

In `ResourcesModule.java`, remove these three import lines (around lines 23–25):

```java
import com.mapcode.services.jmx.SystemMetricsAgent;
import com.mapcode.services.metrics.SystemMetrics;
import com.mapcode.services.metrics.SystemMetricsCollector;
```

- [ ] **Step 2: Remove JMX bindings from ResourcesModule.configure()**

Remove these three lines (around lines 54–56):

```java
        // JMX interface.
        binder.bind(SystemMetricsImpl.class).in(Singleton.class);
        binder.bind(SystemMetricsAgent.class).in(Singleton.class);
```

- [ ] **Step 3: Remove provideSystemMetrics provider method**

Remove:

```java
    @Provides
    @Singleton
    @Nonnull
    public SystemMetrics provideSystemMetrics(
            @Nonnull final SystemMetricsImpl impl) {
        assert impl != null;
        return impl;
    }
```

- [ ] **Step 4: Remove provideSystemMetricsCollector provider method**

Remove:

```java
    @Provides
    @Singleton
    @Nonnull
    public SystemMetricsCollector provideSystemMetricsCollector(
            @Nonnull final SystemMetricsImpl impl) {
        assert impl != null;
        return impl;
    }
```

- [ ] **Step 5: Verify ResourcesModule compiles cleanly**

```bash
grep -n "SystemMetrics\|jolokia\|jmx" \
  service/src/main/java/com/mapcode/services/ResourcesModule.java
```
Expected: no output.

- [ ] **Step 6: Verify build and tests pass**

```bash
mvn test -pl service -am -q 2>&1 | grep -E "BUILD|Tests run.*FAILURE" | tail -5
```
Expected: `BUILD SUCCESS`.

- [ ] **Step 7: Commit**

```bash
git add service/src/main/java/com/mapcode/services/ResourcesModule.java
git commit -m "chore: remove metrics Guice wiring from ResourcesModule

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 5: Delete metrics and JMX class files

**Files:** (all deleted)
- `service/src/main/java/com/mapcode/services/metrics/SystemMetrics.java`
- `service/src/main/java/com/mapcode/services/metrics/SystemMetricsCollector.java`
- `service/src/main/java/com/mapcode/services/implementation/SystemMetricsImpl.java`
- `service/src/main/java/com/mapcode/services/jmx/SystemMetricsAgent.java`

- [ ] **Step 1: Delete all four class files**

```bash
git rm service/src/main/java/com/mapcode/services/metrics/SystemMetrics.java
git rm service/src/main/java/com/mapcode/services/metrics/SystemMetricsCollector.java
git rm service/src/main/java/com/mapcode/services/implementation/SystemMetricsImpl.java
git rm service/src/main/java/com/mapcode/services/jmx/SystemMetricsAgent.java
```

- [ ] **Step 2: Verify no remaining references to these classes**

```bash
grep -r "SystemMetrics\|SystemMetricsCollector\|SystemMetricsImpl\|SystemMetricsAgent" \
  service/src/main/java/ service/src/test/java/
```
Expected: no output.

- [ ] **Step 3: Verify build and tests pass**

```bash
mvn test -pl service -am -q 2>&1 | grep -E "BUILD|Tests run.*FAILURE" | tail -5
```
Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Commit**

```bash
git commit -m "chore: delete SystemMetrics, SystemMetricsCollector, SystemMetricsImpl, SystemMetricsAgent

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 6: Remove monitoring references from README

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Find all monitoring references in README**

```bash
grep -n "metrics\|monitor\|jolokia\|JMX" README.md
```

Expected references (based on current content):
- Line ~23: `"All REST services (except 'metrics') are able to..."` — already fixed in RootResourceImpl HELP_TEXT; README has a separate copy
- Line ~29: `"GET /mapcode/metrics Returns some system metrics..."`
- Line ~287: reference to `/mapcode/metrics` requiring authentication
- Line ~310: example `http get http://localhost:8080/mapcode/metrics ...`
- Lines ~534, ~563: changelog entries mentioning metrics additions

- [ ] **Step 2: Remove or update each reference**

For each reference found:
- Lines documenting `GET /mapcode/metrics` as an endpoint: **remove the line**
- Lines in changelog entries (`* Added metrics for...`, `* Added new metrics:...`): **remove the entire bullet point**
- Any authentication example using `/mapcode/metrics`: **remove the example block**

Read the context around each line before deleting to avoid leaving orphaned structure (e.g., a section with no content after its heading).

- [ ] **Step 3: Verify no monitoring references remain**

```bash
grep -i "metrics\|jolokia\|/monitor" README.md
```
Expected: no output (or only innocent uses of "metrics" in unrelated contexts — inspect each).

- [ ] **Step 4: Verify no monitoring references anywhere in repo**

```bash
git grep -i "jolokia\|SystemMetrics\|monitorclient\|/monitor\b" \
  -- ':!docs/superpowers/specs/2026-05-27-remove-jolokia-monitoring-design.md' \
     ':!docs/superpowers/plans/2026-05-27-remove-jolokia-monitoring.md'
```
Expected: no output.

- [ ] **Step 5: Final build and full test suite**

```bash
mvn package -q 2>&1 | grep -E "BUILD|FAILURE|ERROR" | tail -5
```
Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```bash
git add README.md
git commit -m "docs: remove monitoring and metrics references from README

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Final Checks

- [ ] `git grep -i "jolokia\|SystemMetrics\|monitorclient"` returns no results
- [ ] `mvn package` produces a WAR with no `/monitor/*` or `/mapcode/metrics` routes
- [ ] Close all related beads issues: `bd close <id1> <id2> ...`
