# Remove Jolokia Monitoring — Design Spec

**Date:** 2026-05-27
**Status:** Approved
**Scope:** Full removal of Jolokia, the JMX metrics layer, the `/mapcode/metrics` REST endpoint, and the example monitoring client. No replacement.

---

## Goal

Remove all monitoring infrastructure from mapcode-rest-service: Jolokia HTTP-to-JMX bridge, the `SystemMetrics` JMX MBean, the `/mapcode/metrics` REST endpoint, and the example dashboard client. The result is a service with no monitoring footprint.

---

## What Is Being Removed

| Category | Items |
|----------|-------|
| Jolokia Maven dep | `jolokia-core` in `pom.xml` + `deployment/pom.xml`; `jolokia.version` property |
| Jolokia web config | `JolokiaAgent` servlet + `/monitor/*` mapping in `web.xml` |
| Example client | Entire `example/monitorclient/` directory |
| Metrics call sites | `SystemMetricsCollector` field + injection + all `.increment*()`/`.collect*()` calls in `MapcodeResourceImpl` |
| REST endpoint | `getMetrics(...)` in `RootResource.java`; `SystemMetrics` field + injection + implementation in `RootResourceImpl.java` |
| Metrics classes | `metrics/SystemMetrics.java`, `metrics/SystemMetricsCollector.java`, `implementation/SystemMetricsImpl.java` |
| JMX classes | `jmx/SystemMetricsAgent.java`, `jmx/` package |
| Guice wiring | `SystemMetricsImpl` + `SystemMetricsAgent` bindings; `provideSystemMetrics` + `provideSystemMetricsCollector` providers in `ResourcesModule.java` |
| Standalone wiring | `SystemMetricsImpl` instantiation in `standalone/Server.java` |
| Docs | `/mapcode/metrics` and monitoring references in `README.md` |

---

## Approach: Layered Commits

Six commits, each leaving the project in a compilable state.

### Commit 1 — `chore: remove Jolokia servlet, dependency, and example client`

Files:
- `deployment/src/main/webapp/WEB-INF/web.xml` — remove `JolokiaAgent` servlet + `/monitor/*` mapping
- `pom.xml` — remove `jolokia.version` property; remove `jolokia-core` from `<dependencyManagement>`
- `deployment/pom.xml` — remove `jolokia-core` dependency reference
- Delete `example/monitorclient/` entirely

Build must pass after this commit (Jolokia classes were only used in web.xml config, not in Java code).

### Commit 2 — `refactor: remove metrics collection from MapcodeResourceImpl`

File: `service/src/main/java/com/mapcode/services/implementation/MapcodeResourceImpl.java`

- Remove `SystemMetricsCollector` field declaration
- Remove `SystemMetricsCollector` constructor parameter + assignment
- Remove all calls to the collector (`.incrementNnRequests()` etc.) throughout method bodies

Build must pass: `SystemMetricsCollector` interface still exists but is no longer referenced here.

### Commit 3 — `refactor: remove /mapcode/metrics REST endpoint`

Files:
- `service/src/main/java/com/mapcode/services/RootResource.java` — remove `getMetrics(...)` method declaration and its JAX-RS annotations
- `service/src/main/java/com/mapcode/services/implementation/RootResourceImpl.java` — remove `SystemMetrics` field + constructor parameter + `getMetrics(...)` implementation

Build must pass: `SystemMetrics` interface still exists but is no longer referenced here.

### Commit 4 — `chore: remove metrics Guice wiring and standalone server wiring`

Files:
- `service/src/main/java/com/mapcode/services/ResourcesModule.java` — remove `SystemMetricsImpl` + `SystemMetricsAgent` singleton bindings; remove `provideSystemMetrics(SystemMetricsImpl)` and `provideSystemMetricsCollector(SystemMetricsImpl)` provider methods; remove associated imports
- `service/src/main/java/com/mapcode/services/standalone/Server.java` — remove `SystemMetricsImpl` instantiation and any associated wiring

Build must pass: classes still exist on disk, wiring is now gone.

### Commit 5 — `chore: delete metrics and JMX classes`

Delete entirely:
- `service/src/main/java/com/mapcode/services/metrics/SystemMetrics.java`
- `service/src/main/java/com/mapcode/services/metrics/SystemMetricsCollector.java`
- `service/src/main/java/com/mapcode/services/implementation/SystemMetricsImpl.java`
- `service/src/main/java/com/mapcode/services/jmx/SystemMetricsAgent.java`

Build must pass: no remaining references to these classes after commits 2, 3, and 4.

### Commit 6 — `docs: remove monitoring references from README and docs`

- `README.md` — remove references to `/mapcode/metrics` endpoint and any monitoring instructions
- Any other documentation files referencing Jolokia or metrics

---

## Success Criteria

- `mvn compile` passes after each commit
- `mvn package` produces a deployable WAR with no Jolokia classes or `/monitor` route
- `git grep -i "jolokia\|SystemMetrics\|monitorclient"` returns no results
- No `/monitor/*` or `/mapcode/metrics` routes registered at runtime

---

## Out of Scope

- No replacement monitoring tooling
- No changes to REST API behaviour beyond removing the `/mapcode/metrics` endpoint
