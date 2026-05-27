# Remove Swagger — Design Spec

**Date:** 2026-05-27  
**Status:** Approved  
**Scope:** Full removal of swagger from mapcode-rest-service. No replacement API doc tooling.

---

## Goal

Remove all swagger dependencies, annotations, configuration, UI files, and documentation from the project. The result is a clean codebase with no swagger footprint and no API documentation tooling.

---

## What Is Being Removed

| Category | Items |
|----------|-------|
| Maven deps/plugin | `swagger-jaxrs` dependency, `swagger-maven-plugin`, version properties in `pom.xml` and `service/pom.xml` |
| Java annotations | `@Api`, `@ApiOperation`, `@ApiParam`, `@ApiResponse`, `@ApiResponses` from `MapcodeResource.java`, `RootResource.java`; `@ApiModel`, `@ApiModelProperty` from 11 DTO classes |
| Web config | `ApiListingResource`, `SwaggerSerializers` from `resteasy.providers`; `Swagger` servlet and `/swagger/*` mapping in `web.xml` |
| UI files | Entire `apidocs/swagger/` directory (HTML, JS bundles, CSS, source maps) |
| Generated files | `service/deployment/src/main/webapp/swagger.json` |
| IDE files | `.idea/libraries/Maven__io_swagger_*.xml` (4 files) |
| Docs | `apidocs/README.md` (swagger-only — delete); swagger reference in `docs/superpowers/plans/2026-05-26-territory-lookup.md` |

---

## Approach: Layered Commits

Five commits, each leaving the build in a clean compilable state.

### Commit 1 — `refactor: remove swagger annotations from Java sources`

Files:
- `service/src/main/java/com/mapcode/services/MapcodeResource.java`
- `service/src/main/java/com/mapcode/services/RootResource.java`
- `service/src/main/java/com/mapcode/services/dto/AlphabetDTO.java`
- `service/src/main/java/com/mapcode/services/dto/AlphabetsDTO.java`
- `service/src/main/java/com/mapcode/services/dto/MapcodeDTO.java`
- `service/src/main/java/com/mapcode/services/dto/MapcodesDTO.java`
- `service/src/main/java/com/mapcode/services/dto/PointDTO.java`
- `service/src/main/java/com/mapcode/services/dto/RectangleDTO.java`
- `service/src/main/java/com/mapcode/services/dto/TerritoryDTO.java`
- `service/src/main/java/com/mapcode/services/dto/TerritoriesDTO.java`
- `service/src/main/java/com/mapcode/services/dto/TerritoryCandidateDTO.java`
- `service/src/main/java/com/mapcode/services/dto/TerritoryCandidatesDTO.java`
- `service/src/main/java/com/mapcode/services/dto/VersionDTO.java`

Actions: Remove all `io.swagger.annotations.*` imports and all swagger annotation usages. Classes remain otherwise unchanged.

### Commit 2 — `chore: remove swagger from web.xml`

File: `deployment/src/main/webapp/WEB-INF/web.xml`

Actions:
- Remove `io.swagger.jaxrs.listing.ApiListingResource` and `io.swagger.jaxrs.listing.SwaggerSerializers` from the `resteasy.providers` `<param-value>`
- Remove the `Swagger` `<servlet>` element (name, class, init-params)
- Remove the corresponding `<servlet-mapping>` for `/swagger/*`

### Commit 3 — `chore: remove swagger Maven dependency and plugin`

Files: `pom.xml`, `service/pom.xml`

Actions:
- Remove `swagger.version` property from `pom.xml`
- Remove `swagger-maven-plugin.version` property from `pom.xml`
- Remove `io.swagger:swagger-jaxrs` dependency block from `pom.xml`
- Remove `io.swagger:swagger-jaxrs` dependency reference from `service/pom.xml`
- Remove `swagger-maven-plugin` plugin block (including all configuration and executions) from `service/pom.xml`

### Commit 4 — `chore: delete swagger UI files and generated swagger.json`

Actions:
- Delete `apidocs/swagger/` directory entirely
- Delete `service/deployment/src/main/webapp/swagger.json`
- Delete `.idea/libraries/Maven__io_swagger_swagger_annotations_1_6_2.xml`
- Delete `.idea/libraries/Maven__io_swagger_swagger_core_1_6_2.xml`
- Delete `.idea/libraries/Maven__io_swagger_swagger_models_1_6_2.xml`
- Delete `.idea/libraries/Maven__io_swagger_swagger_jaxrs_1_6_2.xml`

### Commit 5 — `docs: remove swagger references from documentation`

Actions:
- Delete `apidocs/README.md` (file is entirely about swagger UI; no content to preserve)
- Remove swagger annotation pattern references from `docs/superpowers/plans/2026-05-26-territory-lookup.md`

---

## Success Criteria

- `mvn compile` passes after each commit
- `mvn package` produces a deployable WAR with no swagger classes or endpoints
- `git grep -i swagger` returns no results (excluding this spec file and git history)
- No `swagger.*` routes registered at runtime

---

## Out of Scope

- No replacement API documentation tooling
- No changes to REST API behavior, routes, or DTOs (only annotations removed)
