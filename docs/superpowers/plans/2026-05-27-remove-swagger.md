# Remove Swagger Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove all swagger dependencies, annotations, configuration, UI files, and documentation from mapcode-rest-service with no replacement tooling.

**Architecture:** Five sequential commits — annotations first (so the code compiles without swagger-jaxrs), then web.xml config, then Maven deps/plugin, then file deletion, then doc cleanup. Each commit leaves the project in a compilable state.

**Tech Stack:** Java 8, Maven, RESTEasy/JAX-RS, Maven multi-module project (root pom.xml + service/pom.xml)

---

## Files Changed

| File | Action |
|------|--------|
| `service/src/main/java/com/mapcode/services/MapcodeResource.java` | Remove swagger imports + annotations |
| `service/src/main/java/com/mapcode/services/RootResource.java` | Remove swagger imports + annotations |
| `service/src/main/java/com/mapcode/services/dto/AlphabetDTO.java` | Remove swagger imports + annotations |
| `service/src/main/java/com/mapcode/services/dto/AlphabetsDTO.java` | Remove swagger imports + annotations |
| `service/src/main/java/com/mapcode/services/dto/MapcodeDTO.java` | Remove swagger imports + annotations |
| `service/src/main/java/com/mapcode/services/dto/MapcodesDTO.java` | Remove swagger imports + annotations |
| `service/src/main/java/com/mapcode/services/dto/PointDTO.java` | Remove swagger imports + annotations |
| `service/src/main/java/com/mapcode/services/dto/RectangleDTO.java` | Remove swagger imports + annotations |
| `service/src/main/java/com/mapcode/services/dto/TerritoriesDTO.java` | Remove swagger imports + annotations |
| `service/src/main/java/com/mapcode/services/dto/TerritoryCandidateDTO.java` | Remove swagger imports + annotations |
| `service/src/main/java/com/mapcode/services/dto/TerritoryCandidatesDTO.java` | Remove swagger imports + annotations |
| `service/src/main/java/com/mapcode/services/dto/TerritoryDTO.java` | Remove swagger imports + annotations |
| `service/src/main/java/com/mapcode/services/dto/VersionDTO.java` | Remove swagger imports + annotations |
| `deployment/src/main/webapp/WEB-INF/web.xml` | Remove swagger providers + servlet + mapping |
| `pom.xml` | Remove swagger-jaxrs dep + 2 version properties |
| `service/pom.xml` | Remove swagger-jaxrs dep + swagger-maven-plugin block |
| `apidocs/swagger/` (directory) | Delete entirely |
| `service/deployment/src/main/webapp/swagger.json` | Delete |
| `.idea/libraries/Maven__io_swagger_*.xml` (4 files) | Delete |
| `apidocs/README.md` | Delete |
| `docs/superpowers/plans/2026-05-26-territory-lookup.md` | Remove 2 swagger import lines |

---

## Task 1: Remove swagger annotations from Java sources

**Files:** All 13 Java files listed above.

This task uses one shell command to strip every `io.swagger` import line and every `@Api*` annotation line across all files. Annotations may span multiple lines — the multi-line ones are removed manually per file after the single-line pass.

- [ ] **Step 1: Strip single-line swagger imports from all 13 files at once**

```bash
# Run from the repo root: mapcode-rest-service/
# Remove all single-line io.swagger import lines
find service/src/main/java/com/mapcode/services -name "*.java" \
  -exec sed -i '' '/^import io\.swagger\./d' {} +
```

- [ ] **Step 2: Remove the wildcard import in MapcodeResource.java (if not already gone)**

Open `service/src/main/java/com/mapcode/services/MapcodeResource.java` and verify line
`import io.swagger.annotations.*;` is gone. The sed above covers it — just confirm.

- [ ] **Step 3: Remove @Api annotation from MapcodeResource.java**

In `service/src/main/java/com/mapcode/services/MapcodeResource.java`, remove this line (appears just before `@Path("/mapcode")`):

```java
@Api(value = "mapcode", description = "This resource provides the Mapcode REST API.")
```

- [ ] **Step 4: Remove all @ApiOperation and @ApiParam annotations from MapcodeResource.java**

These annotations appear on each method declaration. Remove every `@ApiOperation(...)`, `@ApiParam(...)` annotation block. They are purely decorative — no method signature changes.

Use this command to see all remaining swagger annotation lines:
```bash
grep -n "@Api" service/src/main/java/com/mapcode/services/MapcodeResource.java
```

Delete every line (and its continuation lines) that starts with `@ApiOperation`, `@ApiParam`, or `@ApiImplicitParam`. Method `@GET`, `@POST`, `@Path`, `@Produces` etc. are JAX-RS — leave those.

- [ ] **Step 5: Remove @Api annotation from RootResource.java**

In `service/src/main/java/com/mapcode/services/RootResource.java`, remove this line (just before `@Path("/mapcode")`):

```java
@Api(value = "monitoring", description = "These resources are provided for monitoring purposes.")
```

- [ ] **Step 6: Remove @ApiOperation and @ApiResponses from RootResource.java**

```bash
grep -n "@Api" service/src/main/java/com/mapcode/services/RootResource.java
```

Remove every `@ApiOperation(...)`, `@ApiResponses(...)`, `@ApiResponse(...)` annotation block from method declarations. Leave `@GET`, `@Path`, `@Produces` etc.

- [ ] **Step 7: Remove @ApiModel and @ApiModelProperty from all 11 DTO files**

Each DTO has the same pattern:
- One `@ApiModel(value = "...", description = "...")` block directly above the class declaration
- One or more `@ApiModelProperty(value = "...", ...)` blocks above each field

For each DTO file, run:
```bash
grep -n "@Api" service/src/main/java/com/mapcode/services/dto/AlphabetDTO.java
grep -n "@Api" service/src/main/java/com/mapcode/services/dto/AlphabetsDTO.java
grep -n "@Api" service/src/main/java/com/mapcode/services/dto/MapcodeDTO.java
grep -n "@Api" service/src/main/java/com/mapcode/services/dto/MapcodesDTO.java
grep -n "@Api" service/src/main/java/com/mapcode/services/dto/PointDTO.java
grep -n "@Api" service/src/main/java/com/mapcode/services/dto/RectangleDTO.java
grep -n "@Api" service/src/main/java/com/mapcode/services/dto/TerritoriesDTO.java
grep -n "@Api" service/src/main/java/com/mapcode/services/dto/TerritoryCandidateDTO.java
grep -n "@Api" service/src/main/java/com/mapcode/services/dto/TerritoryCandidatesDTO.java
grep -n "@Api" service/src/main/java/com/mapcode/services/dto/TerritoryDTO.java
grep -n "@Api" service/src/main/java/com/mapcode/services/dto/VersionDTO.java
```

Delete every `@ApiModel(...)` and `@ApiModelProperty(...)` annotation block shown. These are multi-line — delete from the `@ApiModel` or `@ApiModelProperty` line through the closing `)` line.

- [ ] **Step 8: Verify zero swagger references remain in Java sources**

```bash
grep -r "swagger" service/src/main/java/
```

Expected output: nothing. If any lines appear, delete them.

- [ ] **Step 9: Verify the project compiles**

```bash
mvn compile -pl service -am -q
```

Expected: `BUILD SUCCESS` with no errors. If compile fails, check for stray annotation usages with `grep -rn "@Api" service/src/main/java/`.

- [ ] **Step 10: Commit**

```bash
git add service/src/main/java/
git commit -m "refactor: remove swagger annotations from Java sources"
```

---

## Task 2: Remove swagger config from web.xml

**Files:** `deployment/src/main/webapp/WEB-INF/web.xml`

- [ ] **Step 1: Remove swagger providers from the resteasy.providers context-param**

In `deployment/src/main/webapp/WEB-INF/web.xml`, find this `<context-param>` block (lines 40–47):

```xml
    <context-param>
        <param-name>resteasy.providers</param-name>
        <param-value>
            io.swagger.jaxrs.listing.ApiListingResource,
            io.swagger.jaxrs.listing.SwaggerSerializers,
            com.tomtom.speedtools.rest.security.CorsFeature
        </param-value>
    </context-param>
```

Replace it with:

```xml
    <context-param>
        <param-name>resteasy.providers</param-name>
        <param-value>
            com.tomtom.speedtools.rest.security.CorsFeature
        </param-value>
    </context-param>
```

- [ ] **Step 2: Remove the Swagger servlet definition**

Remove this block (lines 76–84):

```xml
    <servlet>
        <servlet-name>Swagger</servlet-name>
        <servlet-class>io.swagger.jaxrs.config.DefaultJaxrsConfig</servlet-class>
        <init-param>
            <param-name>api.version</param-name>
            <param-value>${pom.version}</param-value>
        </init-param>
        <load-on-startup>2</load-on-startup>
    </servlet>
```

- [ ] **Step 3: Remove the Swagger servlet-mapping**

Remove this block (lines 86–89):

```xml
    <servlet-mapping>
        <servlet-name>Swagger</servlet-name>
        <url-pattern>/swagger/*</url-pattern>
    </servlet-mapping>
```

- [ ] **Step 4: Verify no swagger references remain in web.xml**

```bash
grep -i "swagger" deployment/src/main/webapp/WEB-INF/web.xml
```

Expected output: nothing.

- [ ] **Step 5: Commit**

```bash
git add deployment/src/main/webapp/WEB-INF/web.xml
git commit -m "chore: remove swagger from web.xml"
```

---

## Task 3: Remove swagger Maven dependency and plugin

**Files:** `pom.xml`, `service/pom.xml`

- [ ] **Step 1: Remove swagger-maven-plugin.version property from root pom.xml**

In `pom.xml`, remove this line (around line 89):

```xml
        <swagger-maven-plugin.version>3.1.8</swagger-maven-plugin.version>
```

- [ ] **Step 2: Remove swagger.version property from root pom.xml**

In `pom.xml`, remove this line (around line 115):

```xml
        <swagger.version>1.6.6</swagger.version>
```

- [ ] **Step 3: Remove swagger-jaxrs dependency block from root pom.xml**

In `pom.xml` under `<dependencyManagement>`, remove this entire block (around lines 273–283):

```xml
            <dependency>
                <groupId>io.swagger</groupId>
                <artifactId>swagger-jaxrs</artifactId>
                <version>${swagger.version}</version>
                <exclusions>
                    <exclusion>
                        <groupId>javax.ws.rs</groupId>
                        <artifactId>jsr311-api</artifactId>
                    </exclusion>
                </exclusions>
            </dependency>
```

- [ ] **Step 4: Remove swagger-jaxrs dependency from service/pom.xml**

In `service/pom.xml`, remove this block (around lines 146–149):

```xml
        <dependency>
            <groupId>io.swagger</groupId>
            <artifactId>swagger-jaxrs</artifactId>
        </dependency>
```

- [ ] **Step 5: Remove swagger-maven-plugin block from service/pom.xml**

In `service/pom.xml`, remove the entire plugin block including the comment above it (around lines 252–320):

```xml
        <!-- Swagger API documentation. -->
        <plugins>
            <plugin>
                <groupId>com.github.kongchen</groupId>
                <artifactId>swagger-maven-plugin</artifactId>
                ...
            </plugin>
        </plugins>
```

The exact block to remove starts at `<!-- Swagger API documentation. -->` and ends at the closing `</plugins>` tag. The `<build>` element still contains `<resources>` above this — leave that intact.

After removal, `service/pom.xml` `<build>` section should contain only the `<resources>` block and no `<plugins>` block.

- [ ] **Step 6: Verify no swagger references remain in either pom**

```bash
grep -i "swagger" pom.xml service/pom.xml
```

Expected output: nothing.

- [ ] **Step 7: Verify the project compiles cleanly without the swagger dependency**

```bash
mvn compile -pl service -am -q
```

Expected: `BUILD SUCCESS`. If you see `package io.swagger does not exist`, a swagger annotation was missed in Task 1 — go back and remove it.

- [ ] **Step 8: Commit**

```bash
git add pom.xml service/pom.xml
git commit -m "chore: remove swagger Maven dependency and plugin"
```

---

## Task 4: Delete swagger UI files and generated artifacts

**No compilation required — these are static files and IDE metadata.**

- [ ] **Step 1: Delete the swagger UI directory**

```bash
git rm -r apidocs/swagger/
```

This removes all 12 files: `index.html`, `oauth2-redirect.html`, `favicon-16x16.png`, `favicon-32x32.png`, `swagger-ui.js`, `swagger-ui.js.map`, `swagger-ui-bundle.js`, `swagger-ui-bundle.js.map`, `swagger-ui-standalone-preset.js`, `swagger-ui-standalone-preset.js.map`, `swagger-ui.css`, `swagger-ui.css.map`.

- [ ] **Step 2: Delete the tracked generated swagger.json**

```bash
git rm service/deployment/src/main/webapp/swagger.json
```

- [ ] **Step 3: Delete the IntelliJ swagger library XML files**

```bash
git rm .idea/libraries/Maven__io_swagger_swagger_annotations_1_6_2.xml
git rm .idea/libraries/Maven__io_swagger_swagger_core_1_6_2.xml
git rm .idea/libraries/Maven__io_swagger_swagger_jaxrs_1_6_2.xml
git rm .idea/libraries/Maven__io_swagger_swagger_models_1_6_2.xml
```

- [ ] **Step 4: Verify all target files are staged for deletion**

```bash
git status
```

Expected: all 17 files listed under "Changes to be committed" as `deleted:`.

- [ ] **Step 5: Commit**

```bash
git commit -m "chore: delete swagger UI files, generated swagger.json, and IDE library stubs"
```

---

## Task 5: Clean up documentation

**Files:** `apidocs/README.md`, `docs/superpowers/plans/2026-05-26-territory-lookup.md`

- [ ] **Step 1: Delete apidocs/README.md**

The file is entirely about swagger UI — there is nothing to preserve.

```bash
git rm apidocs/README.md
```

- [ ] **Step 2: Remove swagger import lines from the territory-lookup plan**

In `docs/superpowers/plans/2026-05-26-territory-lookup.md`, remove these two lines (around lines 289–290):

```
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
```

These appear in a code sample block in the plan. Delete only those two lines; leave the surrounding code sample intact.

- [ ] **Step 3: Verify zero swagger references remain in the entire repo**

```bash
git grep -i "swagger" -- ':!docs/superpowers/specs/2026-05-27-remove-swagger-design.md' \
                         ':!docs/superpowers/plans/2026-05-27-remove-swagger.md'
```

Expected output: nothing. (The two spec/plan files for this task are excluded since they legitimately reference swagger by name.)

- [ ] **Step 4: Commit**

```bash
git add docs/superpowers/plans/2026-05-26-territory-lookup.md
git commit -m "docs: remove swagger references from documentation"
```

---

## Final Verification

- [ ] **Full build passes**

```bash
mvn package -q
```

Expected: `BUILD SUCCESS`. The resulting WAR in `deployment/target/` should contain no swagger classes or routes.

- [ ] **No swagger routes at runtime**

If you can start the service locally, confirm that `GET /swagger.json` and `GET /swagger/` return 404.

- [ ] **Close the beads issue**

```bash
bd close <issue-id>
```
