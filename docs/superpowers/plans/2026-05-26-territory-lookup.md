# Territory Lookup Endpoint — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `GET /mapcode/codes/{lat},{lon}/territories` that returns the ranked "most likely" mapcode territories for a point, backed by OSM admin-boundary data loaded from a sidecar FlatGeobuf file at startup.

**Architecture:** A new `BoundaryService` (concrete class, eager singleton) loads a configured `.fgb` file into a JTS `STRtree` at construction. The new endpoint on `MapcodeResourceImpl` calls `BoundaryService.lookup(lat, lon)` to obtain a list of `TerritoryMatch` records, sorts by admin level then polygon area, and serializes via two new DTOs. A Python build script in `tools/` produces the production `.fgb` from OSM extracts; a second Python helper produces a tiny synthetic `.fgb` checked into test resources.

**Tech Stack:**
- Java 8 (matches existing service)
- JTS Topology Suite (`org.locationtech.jts:jts-core`) — `STRtree`, `Geometry.contains`, `Geometry.getArea`
- FlatGeobuf Java reader (`org.wololo:flatgeobuf`)
- Existing: JAX-RS, RESTEasy, Jackson, JAX-B, Guice, Akka-based async (`ResourceProcessor`), JUnit 4

**Spec:** `docs/superpowers/specs/2026-05-26-territory-lookup-design.md`

---

## File Structure

### New files

**Production code (`service/src/main/java/`):**

- `com/mapcode/services/dto/TerritoryCandidateDTO.java` — single result entry; mirrors `TerritoryDTO`'s annotation pattern.
- `com/mapcode/services/dto/TerritoryCandidateListDTO.java` — list wrapper; mirrors `TerritoryListDTO`.
- `com/mapcode/services/implementation/TerritoryMatch.java` — immutable value object returned by `BoundaryService` (not a DTO).
- `com/mapcode/services/implementation/BoundaryService.java` — loads the `.fgb`, holds a JTS `STRtree`, exposes `lookup(lat, lon)`. Fails fast if file is missing/unreadable.

**Tests (`service/src/test/java/`):**

- `com/mapcode/services/implementation/BoundaryServiceTest.java` — unit tests for the loader and lookup logic, pointing at the synthetic `.fgb`.
- `com/mapcode/services/ApiCodesTerritoriesTest.java` — end-to-end HTTP tests for the new endpoint, using `LocalTestServer`.

**Test fixtures (`service/src/test/resources/`):**

- `borders-test.fgb` — tiny synthetic FlatGeobuf with hand-built polygons. Binary, regeneratable from `tools/build-test-borders.py`.

**Tooling (`tools/`):**

- `tools/README.md` — operator-facing docs for both scripts.
- `tools/build-borders.py` — production pipeline (OSM extract → simplified FlatGeobuf with mapcode `alphaCode` tagged).
- `tools/build-test-borders.py` — hand-built synthetic polygons → `service/src/test/resources/borders-test.fgb`.

### Modified files

- `service/pom.xml` — add JTS + FlatGeobuf dependencies.
- `service/src/main/java/com/mapcode/services/MapcodeResource.java` — add new interface method.
- `service/src/main/java/com/mapcode/services/implementation/MapcodeResourceImpl.java` — add `BoundaryService` constructor arg + new endpoint impl.
- `service/src/main/java/com/mapcode/services/metrics/SystemMetricsCollector.java` — add new request-counter methods.
- `service/src/main/java/com/mapcode/services/metrics/SystemMetrics.java` — expose new metrics + enum entries (for JMX parity).
- `service/src/main/java/com/mapcode/services/implementation/SystemMetricsImpl.java` — implement new counters.
- `service/src/main/java/com/mapcode/services/ResourcesModule.java` — bind `BoundaryService` as eager singleton.
- `service/src/main/java/com/mapcode/services/implementation/RootResourceImpl.java` — add new route to the help page text.
- `service/src/test/java/com/mapcode/services/LocalTestServer.java` — construct a `BoundaryService` from the test fixture and inject it.

---

## Tasks

### Task 1: Add JTS and FlatGeobuf dependencies

**Files:**
- Modify: `service/pom.xml`

- [ ] **Step 1: Add the two dependency entries**

In `service/pom.xml`, insert these two `<dependency>` blocks inside the existing `<dependencies>` element (e.g., after the `mapcode` dependency at lines 84–88, alphabetical order is not strict in this file but co-locating with `mapcode` is fine):

```xml
        <dependency>
            <groupId>org.locationtech.jts</groupId>
            <artifactId>jts-core</artifactId>
            <version>1.19.0</version>
        </dependency>

        <dependency>
            <groupId>org.wololo</groupId>
            <artifactId>flatgeobuf</artifactId>
            <version>3.27.1</version>
        </dependency>
```

- [ ] **Step 2: Verify the project still builds**

Run: `mvn -pl service -am compile -q`
Expected: build succeeds, no compile errors. New jars resolved.

- [ ] **Step 3: Commit**

```bash
git add service/pom.xml
git commit -m "Add JTS and FlatGeobuf dependencies for OSM-backed territory lookup"
```

---

### Task 2: Synthetic test fixture script

**Files:**
- Create: `tools/build-test-borders.py`
- Create: `tools/README.md`
- Create: `service/src/test/resources/borders-test.fgb` (binary, generated by the script)

- [ ] **Step 1: Create `tools/build-test-borders.py`**

```python
#!/usr/bin/env python3
"""Build a tiny synthetic borders FlatGeobuf used by unit tests.

Polygons:
  - NLD            country, square covering roughly the Netherlands.
  - USA            country, square covering the contiguous US.
  - USA-CA         subdivision inside USA, square covering California.
  - DISPUTED-A     country square overlapping DISPUTED-B (simulates a disputed region).
  - DISPUTED-B     country square overlapping DISPUTED-A; smaller area so it ranks first.
  - NO-MAPCODE-SUB subdivision whose alphaCode falls back to NO-MAPCODE-PARENT
                   (simulates the "no subdivision mapcode equivalent" case).

Run:
    python3 tools/build-test-borders.py
Output:
    service/src/test/resources/borders-test.fgb
"""
import pathlib
from flatgeobuf import FlatGeobufWriter  # requires `pip install flatgeobuf`
from shapely.geometry import Polygon, mapping

OUT = pathlib.Path(__file__).resolve().parent.parent / "service" / "src" / "test" / "resources" / "borders-test.fgb"

# (alphaCode, parentAlphaCode or None, adminLevel, polygon)
FEATURES = [
    ("NLD", None,  2, Polygon([(3.0, 50.5), (7.5, 50.5), (7.5, 53.7), (3.0, 53.7)])),
    ("USA", None,  2, Polygon([(-125.0, 24.0), (-66.0, 24.0), (-66.0, 49.0), (-125.0, 49.0)])),
    ("USA-CA", "USA", 4, Polygon([(-124.0, 32.5), (-114.0, 32.5), (-114.0, 42.0), (-124.0, 42.0)])),
    ("DISPUTED-A", None, 2, Polygon([(100.0, 0.0), (110.0, 0.0), (110.0, 10.0), (100.0, 10.0)])),
    ("DISPUTED-B", None, 2, Polygon([(105.0, 5.0), (108.0, 5.0), (108.0, 8.0), (105.0, 8.0)])),
    ("NO-MAPCODE-PARENT", None, 2, Polygon([(20.0, 60.0), (25.0, 60.0), (25.0, 65.0), (20.0, 65.0)])),
    # Subdivision sized to fully fit inside its parent; build script in prod would have
    # already collapsed alphaCode to the parent. Here we simulate that collapse: it's tagged
    # as the parent code so the loader treats them identically.
    ("NO-MAPCODE-PARENT", None, 4, Polygon([(21.0, 61.0), (24.0, 61.0), (24.0, 64.0), (21.0, 64.0)])),
]

with FlatGeobufWriter(
    OUT,
    geometry_type="Polygon",
    properties={
        "alphaCode": "String",
        "parentAlphaCode": "String",
        "adminLevel": "Int",
        "area": "Double",
    },
) as w:
    for alpha, parent, level, poly in FEATURES:
        w.add(
            geometry=mapping(poly),
            properties={
                "alphaCode": alpha,
                "parentAlphaCode": parent or "",
                "adminLevel": level,
                "area": poly.area,
            },
        )

print(f"Wrote {OUT}")
```

- [ ] **Step 2: Create `tools/README.md`**

```markdown
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

Run:
    python3 tools/build-borders.py --osm <osm-pbf-or-source> --out borders.fgb

Then upload the resulting file to wherever deployments fetch it from
and point the service at it via `-Dmapcode.borders.path=/path/to/borders.fgb`.

## build-test-borders.py — synthetic test fixture

Regenerates `service/src/test/resources/borders-test.fgb` from a small
hand-built set of polygons. Re-run only when the fixture needs to change.

Run:
    python3 tools/build-test-borders.py

Requirements (both scripts):
    pip install flatgeobuf shapely
```

- [ ] **Step 3: Generate the fixture and verify it exists**

Run:
```bash
pip install flatgeobuf shapely
python3 tools/build-test-borders.py
ls -lh service/src/test/resources/borders-test.fgb
```
Expected: `borders-test.fgb` exists, non-empty (a few KB).

- [ ] **Step 4: Commit**

```bash
git add tools/build-test-borders.py tools/README.md service/src/test/resources/borders-test.fgb
git commit -m "Add synthetic borders fixture and tools README"
```

---

### Task 3: `TerritoryCandidateDTO`

**Files:**
- Create: `service/src/main/java/com/mapcode/services/dto/TerritoryCandidateDTO.java`
- Create: `service/src/test/java/com/mapcode/services/dto/TerritoryCandidateDTOTest.java`

- [ ] **Step 1: Write the failing test**

Create `service/src/test/java/com/mapcode/services/dto/TerritoryCandidateDTOTest.java`:

```java
/*
 * Copyright (C) 2016-2026, Stichting Mapcode Foundation (http://www.mapcode.com)
 */
package com.mapcode.services.dto;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TerritoryCandidateDTOTest {

    @Test
    public void validateSubdivisionWithParent() {
        final TerritoryCandidateDTO dto = new TerritoryCandidateDTO("USA-CA", "USA");
        dto.validate();
        assertEquals("USA-CA", dto.getAlphaCode());
        assertEquals("USA", dto.getParentAlphaCode());
    }

    @Test
    public void validateCountryWithoutParent() {
        final TerritoryCandidateDTO dto = new TerritoryCandidateDTO("NLD", null);
        dto.validate();
        assertEquals("NLD", dto.getAlphaCode());
        assertNull(dto.getParentAlphaCode());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl service test -Dtest=TerritoryCandidateDTOTest -q`
Expected: FAIL with compile error: `cannot find symbol class TerritoryCandidateDTO`.

- [ ] **Step 3: Write the DTO**

Create `service/src/main/java/com/mapcode/services/dto/TerritoryCandidateDTO.java`:

```java
/*
 * Copyright (C) 2016-2026, Stichting Mapcode Foundation (http://www.mapcode.com)
 */
package com.mapcode.services.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.mapcode.services.ApiConstants;
import com.tomtom.speedtools.apivalidation.ApiDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.annotation.*;

@SuppressWarnings({"NullableProblems", "InstanceVariableMayNotBeInitialized"})
@ApiModel(
        value = "territoryCandidate",
        description = "A single territory candidate returned by territory lookup for a lat/lon.")
@JsonInclude(Include.NON_EMPTY)
@XmlRootElement(name = "territoryCandidate")
@XmlAccessorType(XmlAccessType.FIELD)
public final class TerritoryCandidateDTO extends ApiDTO {

    @ApiModelProperty(
            name = "alphaCode",
            value = "The mapcode alpha-code of the territory (3-character `XXX` or `XX-YY`).")
    @XmlElement(name = "alphaCode")
    @Nonnull
    private String alphaCode;

    @ApiModelProperty(
            name = "parentAlphaCode",
            value = "(optional) The mapcode alpha-code of the parent country. Omitted when not applicable.")
    @XmlElement(name = "parentAlphaCode")
    @Nullable
    private String parentAlphaCode;

    @Override
    public void validate() {
        validator().start();
        validator().checkString(true, "alphaCode", alphaCode,
                ApiConstants.API_NAME_LEN_MIN, ApiConstants.API_NAME_LEN_MAX);
        validator().checkString(false, "parentAlphaCode", parentAlphaCode,
                ApiConstants.API_NAME_LEN_MIN, ApiConstants.API_NAME_LEN_MAX);
        validator().done();
    }

    public TerritoryCandidateDTO(@Nonnull final String alphaCode,
                                 @Nullable final String parentAlphaCode) {
        this.alphaCode = alphaCode;
        this.parentAlphaCode = parentAlphaCode;
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    private TerritoryCandidateDTO() {
        // Default constructor required by JAX-B.
        super();
    }

    @Nonnull
    public String getAlphaCode() {
        beforeGet();
        return alphaCode;
    }

    public void setAlphaCode(@Nonnull final String alphaCode) {
        beforeSet();
        assert alphaCode != null;
        this.alphaCode = alphaCode;
    }

    @Nullable
    public String getParentAlphaCode() {
        beforeGet();
        return parentAlphaCode;
    }

    public void setParentAlphaCode(@Nullable final String parentAlphaCode) {
        beforeSet();
        this.parentAlphaCode = parentAlphaCode;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl service test -Dtest=TerritoryCandidateDTOTest -q`
Expected: PASS, 2 tests run.

- [ ] **Step 5: Commit**

```bash
git add service/src/main/java/com/mapcode/services/dto/TerritoryCandidateDTO.java service/src/test/java/com/mapcode/services/dto/TerritoryCandidateDTOTest.java
git commit -m "Add TerritoryCandidateDTO for territory lookup responses"
```

---

### Task 4: `TerritoryCandidateListDTO`

**Files:**
- Create: `service/src/main/java/com/mapcode/services/dto/TerritoryCandidateListDTO.java`
- Create: `service/src/test/java/com/mapcode/services/dto/TerritoryCandidateListDTOTest.java`

- [ ] **Step 1: Write the failing test**

Create `service/src/test/java/com/mapcode/services/dto/TerritoryCandidateListDTOTest.java`:

```java
/*
 * Copyright (C) 2016-2026, Stichting Mapcode Foundation (http://www.mapcode.com)
 */
package com.mapcode.services.dto;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class TerritoryCandidateListDTOTest {

    @Test
    public void validateEmpty() {
        final TerritoryCandidateListDTO dto = new TerritoryCandidateListDTO(Collections.emptyList());
        dto.validate();
        assertEquals(0, dto.size());
    }

    @Test
    public void validateWithEntries() {
        final TerritoryCandidateListDTO dto = new TerritoryCandidateListDTO(Arrays.asList(
                new TerritoryCandidateDTO("USA-CA", "USA"),
                new TerritoryCandidateDTO("USA", null)));
        dto.validate();
        assertEquals(2, dto.size());
        assertEquals("USA-CA", dto.get(0).getAlphaCode());
        assertEquals("USA", dto.get(1).getAlphaCode());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl service test -Dtest=TerritoryCandidateListDTOTest -q`
Expected: FAIL with compile error: `cannot find symbol class TerritoryCandidateListDTO`.

- [ ] **Step 3: Write the list DTO**

Create `service/src/main/java/com/mapcode/services/dto/TerritoryCandidateListDTO.java`:

```java
/*
 * Copyright (C) 2016-2026, Stichting Mapcode Foundation (http://www.mapcode.com)
 */
package com.mapcode.services.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.tomtom.speedtools.apivalidation.ApiListDTO;

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@SuppressWarnings("NullableProblems")
@JsonInclude(Include.NON_EMPTY)
@XmlRootElement(name = "territories")
@XmlAccessorType(XmlAccessType.FIELD)
public final class TerritoryCandidateListDTO extends ApiListDTO<TerritoryCandidateDTO> {

    @Override
    public void validateOne(@Nonnull final TerritoryCandidateDTO elm) {
        validator().checkNotNullAndValidate(true, "territoryCandidate", elm);
    }

    public TerritoryCandidateListDTO(@Nonnull final List<TerritoryCandidateDTO> candidates) {
        super(candidates);
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    private TerritoryCandidateListDTO() {
        // Default constructor required by JAX-B.
        super();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl service test -Dtest=TerritoryCandidateListDTOTest -q`
Expected: PASS, 2 tests run.

- [ ] **Step 5: Commit**

```bash
git add service/src/main/java/com/mapcode/services/dto/TerritoryCandidateListDTO.java service/src/test/java/com/mapcode/services/dto/TerritoryCandidateListDTOTest.java
git commit -m "Add TerritoryCandidateListDTO for territory lookup responses"
```

---

### Task 5: `TerritoryMatch` value object

**Files:**
- Create: `service/src/main/java/com/mapcode/services/implementation/TerritoryMatch.java`

This is a small immutable carrier between `BoundaryService` and the resource impl. No tests on its own — it's exercised in `BoundaryServiceTest`.

- [ ] **Step 1: Create the class**

Create `service/src/main/java/com/mapcode/services/implementation/TerritoryMatch.java`:

```java
/*
 * Copyright (C) 2016-2026, Stichting Mapcode Foundation (http://www.mapcode.com)
 */
package com.mapcode.services.implementation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * One polygon containing a lat/lon point, as returned by {@link BoundaryService}.
 * Immutable.
 */
public final class TerritoryMatch {

    @Nonnull private final String alphaCode;
    @Nullable private final String parentAlphaCode;
    private final int adminLevel;
    private final double area;

    public TerritoryMatch(
            @Nonnull final String alphaCode,
            @Nullable final String parentAlphaCode,
            final int adminLevel,
            final double area) {
        this.alphaCode = alphaCode;
        this.parentAlphaCode = parentAlphaCode;
        this.adminLevel = adminLevel;
        this.area = area;
    }

    @Nonnull
    public String getAlphaCode() {
        return alphaCode;
    }

    @Nullable
    public String getParentAlphaCode() {
        return parentAlphaCode;
    }

    public int getAdminLevel() {
        return adminLevel;
    }

    public double getArea() {
        return area;
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `mvn -pl service compile -q`
Expected: build succeeds.

- [ ] **Step 3: Commit**

```bash
git add service/src/main/java/com/mapcode/services/implementation/TerritoryMatch.java
git commit -m "Add TerritoryMatch value object"
```

---

### Task 6: `BoundaryService` — file load + lookup

**Files:**
- Create: `service/src/main/java/com/mapcode/services/implementation/BoundaryService.java`
- Create: `service/src/test/java/com/mapcode/services/implementation/BoundaryServiceTest.java`

- [ ] **Step 1: Write failing tests against the synthetic fixture**

Create `service/src/test/java/com/mapcode/services/implementation/BoundaryServiceTest.java`:

```java
/*
 * Copyright (C) 2016-2026, Stichting Mapcode Foundation (http://www.mapcode.com)
 */
package com.mapcode.services.implementation;

import org.junit.Test;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BoundaryServiceTest {

    private static final Path FIXTURE = Paths.get("src", "test", "resources", "borders-test.fgb");

    @Test
    public void pointInsideCountryReturnsCountryOnly() {
        final BoundaryService svc = new BoundaryService(FIXTURE.toString());
        final List<TerritoryMatch> matches = svc.lookup(52.0, 5.0); // inside NLD square
        assertEquals(1, matches.size());
        assertEquals("NLD", matches.get(0).getAlphaCode());
        assertNull(matches.get(0).getParentAlphaCode());
        assertEquals(2, matches.get(0).getAdminLevel());
    }

    @Test
    public void pointInsideSubdivisionReturnsSubdivisionBeforeCountry() {
        final BoundaryService svc = new BoundaryService(FIXTURE.toString());
        final List<TerritoryMatch> matches = svc.lookup(36.0, -120.0); // inside USA-CA square
        assertEquals(2, matches.size());
        assertEquals("USA-CA", matches.get(0).getAlphaCode());
        assertEquals("USA", matches.get(0).getParentAlphaCode());
        assertEquals(4, matches.get(0).getAdminLevel());
        assertEquals("USA", matches.get(1).getAlphaCode());
        assertEquals(2, matches.get(1).getAdminLevel());
    }

    @Test
    public void pointAtSeaReturnsEmptyList() {
        final BoundaryService svc = new BoundaryService(FIXTURE.toString());
        final List<TerritoryMatch> matches = svc.lookup(0.0, -30.0); // mid-Atlantic
        assertTrue(matches.isEmpty());
    }

    @Test
    public void disputedRegionReturnsSmallerPolygonFirst() {
        final BoundaryService svc = new BoundaryService(FIXTURE.toString());
        // (106.5, 6.5) is inside both DISPUTED-A (large) and DISPUTED-B (small).
        final List<TerritoryMatch> matches = svc.lookup(6.5, 106.5);
        assertEquals(2, matches.size());
        assertEquals("DISPUTED-B", matches.get(0).getAlphaCode());
        assertEquals("DISPUTED-A", matches.get(1).getAlphaCode());
    }

    @Test(expected = IllegalStateException.class)
    public void missingFileFailsConstruction() {
        new BoundaryService("/does/not/exist.fgb");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl service test -Dtest=BoundaryServiceTest -q`
Expected: FAIL with compile error: `cannot find symbol class BoundaryService`.

- [ ] **Step 3: Write `BoundaryService`**

Create `service/src/main/java/com/mapcode/services/implementation/BoundaryService.java`:

```java
/*
 * Copyright (C) 2016-2026, Stichting Mapcode Foundation (http://www.mapcode.com)
 */
package com.mapcode.services.implementation;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.index.strtree.STRtree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wololo.flatgeobuf.HeaderMeta;
import org.wololo.flatgeobuf.geotools.FeatureCollectionConversions;
import org.wololo.flatgeobuf.geotools.FlatGeobufFeatureIterator;

import javax.annotation.Nonnull;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Loads OSM admin-boundary polygons from a FlatGeobuf sidecar file and answers
 * point-in-polygon lookups. Eager singleton: the constructor reads and indexes
 * the entire file. Missing or unreadable files cause an {@link IllegalStateException}
 * (the service is then expected to fail fast).
 */
public class BoundaryService {

    private static final Logger LOG = LoggerFactory.getLogger(BoundaryService.class);
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    private final STRtree index;

    public BoundaryService(@Nonnull final String bordersFilePath) {
        final Path path = Paths.get(bordersFilePath);
        if (!Files.isReadable(path)) {
            throw new IllegalStateException("Borders file not readable: " + path);
        }
        this.index = new STRtree();
        int loaded = 0;
        try (InputStream in = new FileInputStream(path.toFile())) {
            final HeaderMeta header = HeaderMeta.read(in);
            final FlatGeobufFeatureIterator it =
                    new FlatGeobufFeatureIterator(in, header, null);
            while (it.hasNext()) {
                final org.opengis.feature.simple.SimpleFeature f = it.next();
                final Geometry geom = (Geometry) f.getDefaultGeometry();
                if (geom == null) {
                    continue;
                }
                final String alphaCode = (String) f.getAttribute("alphaCode");
                final Object parentRaw = f.getAttribute("parentAlphaCode");
                final String parentAlphaCode =
                        (parentRaw == null || ((String) parentRaw).isEmpty())
                                ? null
                                : (String) parentRaw;
                final int adminLevel = ((Number) f.getAttribute("adminLevel")).intValue();
                final double area = ((Number) f.getAttribute("area")).doubleValue();
                final IndexedEntry entry =
                        new IndexedEntry(geom, alphaCode, parentAlphaCode, adminLevel, area);
                index.insert(geom.getEnvelopeInternal(), entry);
                loaded++;
            }
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to load borders file: " + path, e);
        }
        index.build();
        LOG.info("BoundaryService: loaded {} polygons from {}", loaded, path);
    }

    /**
     * Look up all territories whose polygon contains {@code (lat, lon)}. Returns
     * a list sorted by admin level ascending-by-specificity (level 4 first, then 2),
     * with smaller polygon area first within each level.
     */
    @Nonnull
    public List<TerritoryMatch> lookup(final double latDeg, final double lonDeg) {
        final Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(lonDeg, latDeg));
        @SuppressWarnings("unchecked")
        final List<IndexedEntry> candidates = index.query(point.getEnvelopeInternal());
        final List<TerritoryMatch> hits = new ArrayList<>(candidates.size());
        for (final IndexedEntry e : candidates) {
            if (e.geometry.contains(point)) {
                hits.add(new TerritoryMatch(e.alphaCode, e.parentAlphaCode, e.adminLevel, e.area));
            }
        }
        hits.sort(Comparator
                .comparingInt(TerritoryMatch::getAdminLevel).reversed() // level 4 before level 2
                .thenComparingDouble(TerritoryMatch::getArea));         // smaller area first
        return hits;
    }

    private static final class IndexedEntry {
        @Nonnull final Geometry geometry;
        @Nonnull final String alphaCode;
        final String parentAlphaCode;
        final int adminLevel;
        final double area;

        IndexedEntry(
                @Nonnull final Geometry geometry,
                @Nonnull final String alphaCode,
                final String parentAlphaCode,
                final int adminLevel,
                final double area) {
            this.geometry = geometry;
            this.alphaCode = alphaCode;
            this.parentAlphaCode = parentAlphaCode;
            this.adminLevel = adminLevel;
            this.area = area;
        }
    }
}
```

> **Implementation note for the agent:** The exact class/package names from `org.wololo:flatgeobuf` may differ slightly between versions; if `FlatGeobufFeatureIterator` or `HeaderMeta` aren't found at compile time, use the version 3.27.1 javadoc to locate the correct reader entry point. The contract this code relies on is: stream features one-by-one, expose `geometry`/`alphaCode`/`parentAlphaCode`/`adminLevel`/`area`.

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -pl service test -Dtest=BoundaryServiceTest -q`
Expected: all 5 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add service/src/main/java/com/mapcode/services/implementation/BoundaryService.java service/src/test/java/com/mapcode/services/implementation/BoundaryServiceTest.java
git commit -m "Add BoundaryService that loads FlatGeobuf borders and answers point-in-polygon"
```

---

### Task 7: Extend metrics interfaces with lat/lon-to-territories counters

**Files:**
- Modify: `service/src/main/java/com/mapcode/services/metrics/SystemMetricsCollector.java`
- Modify: `service/src/main/java/com/mapcode/services/metrics/SystemMetrics.java`
- Modify: `service/src/main/java/com/mapcode/services/implementation/SystemMetricsImpl.java`

- [ ] **Step 1: Add collector methods**

In `service/src/main/java/com/mapcode/services/metrics/SystemMetricsCollector.java`, append after `addOneTerritoryRequest`:

```java
    /**
     * Called whenever ANY lat/lon to territories request is made.
     *
     * @param client Client that issued the call. Can be null
     */
    public void addOneLatLonToTerritoriesRequest(@Nullable String client);

    /**
     * Called whenever a successful lat/lon to territories request is made.
     *
     * @param client Client that issued the call. Can be null
     */
    public void addOneValidLatLonToTerritoriesRequest(@Nullable String client);
```

- [ ] **Step 2: Add `SystemMetrics` enum entries and accessors**

In `service/src/main/java/com/mapcode/services/metrics/SystemMetrics.java`:

In the `Metric` enum, append before `WARNINGS_AND_ERRORS`:

```java
        ALL_LATLON_TO_TERRITORIES_REQUESTS,
        VALID_LATLON_TO_TERRITORIES_REQUESTS,
```

Append after `getAllTerritoryRequests()`:

```java
    /**
     * @return The total number of requests for lat/lon to territories.
     */
    @Nonnull
    MultiMetricsData getAllLatLonToTerritoriesRequests();

    /**
     * @return The number of valid requests for lat/lon to territories.
     */
    @Nonnull
    MultiMetricsData getValidLatLonToTerritoriesRequests();
```

- [ ] **Step 3: Implement in `SystemMetricsImpl`**

In `service/src/main/java/com/mapcode/services/implementation/SystemMetricsImpl.java`:

After the `warningsAndErrors` field at line 55, add:

```java
    private final MultiMetricsCollector allLatLonToTerritoriesRequests = MultiMetricsCollector.all();
    private final MultiMetricsCollector validLatLonToTerritoriesRequests = MultiMetricsCollector.all();
```

In the `all` `EnumMap` initializer (the `put(...)` block ending around line 78), add the two new entries before `WARNINGS_AND_ERRORS`:

```java
                put(Metric.ALL_LATLON_TO_TERRITORIES_REQUESTS, allLatLonToTerritoriesRequests);
                put(Metric.VALID_LATLON_TO_TERRITORIES_REQUESTS, validLatLonToTerritoriesRequests);
```

After `getAllTerritoryRequests()` (around line 203), add:

```java
    @Nonnull
    @Override
    public MultiMetricsData getAllLatLonToTerritoriesRequests() {
        return allLatLonToTerritoriesRequests;
    }

    @Nonnull
    @Override
    public MultiMetricsData getValidLatLonToTerritoriesRequests() {
        return validLatLonToTerritoriesRequests;
    }
```

After `addOneTerritoryRequest` (around line 283), add:

```java
    @Override
    public void addOneLatLonToTerritoriesRequest(@Nullable final String client) {
        allLatLonToTerritoriesRequests.addValue(1);
    }

    @Override
    public void addOneValidLatLonToTerritoriesRequest(@Nullable final String client) {
        validLatLonToTerritoriesRequests.addValue(1);
    }
```

- [ ] **Step 4: Verify the project compiles**

Run: `mvn -pl service compile -q`
Expected: build succeeds.

- [ ] **Step 5: Commit**

```bash
git add service/src/main/java/com/mapcode/services/metrics/SystemMetricsCollector.java \
        service/src/main/java/com/mapcode/services/metrics/SystemMetrics.java \
        service/src/main/java/com/mapcode/services/implementation/SystemMetricsImpl.java
git commit -m "Add metrics counters for lat/lon-to-territories requests"
```

---

### Task 8: Declare the new endpoint on `MapcodeResource`

**Files:**
- Modify: `service/src/main/java/com/mapcode/services/MapcodeResource.java`

- [ ] **Step 1: Add the new interface method**

In `service/src/main/java/com/mapcode/services/MapcodeResource.java`, add this method to the interface (a good location is right after the existing `convertLatLonToMapcode` methods, before `convertMapcodeToLatLon` at line 200):

```java
    /**
     * Look up the "most likely" territories containing a lat/lon, ranked.
     * Backed by OSM admin-boundary polygons; results are in mapcode alphaCode
     * format (e.g., {@code USA-CA}, {@code NLD}).
     *
     * @param paramLatDegAsString Latitude. Range: [-90, 90].
     * @param paramLonDegAsString Longitude. Range: any double, wrapped to [-180, 180].
     * @param paramClient         Indicator of calling client (for stats).
     * @param paramAllowLog       True if logging is allowed. Default is true.
     * @param response            {@link com.mapcode.services.dto.TerritoryCandidateListDTO}.
     * @throws ApiException API exception, translated into HTTP status code.
     */
    @ApiOperation(
            value = "Look up the ranked list of mapcode territories containing a lat/lon.",
            response = TerritoryCandidateListDTO.class)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Ranked territory candidates (possibly empty for points at sea).",
                    response = TerritoryCandidateListDTO.class),
            @ApiResponse(code = 400, message = "Bad request. For example, a parameter may be out of range.")})
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("codes/{" + PARAM_LAT_DEG + "},{" + PARAM_LON_DEG + "}/territories")
    void getTerritoriesForLatLon(
            @ApiParam(
                    value = "Latitude in degrees. Format: [-90, 90].",
                    allowableValues = "range[-90,90]")
            @PathParam(PARAM_LAT_DEG) @Nullable String paramLatDegAsString,
            @ApiParam(
                    value = "Longitude in degrees. Format: [-180, 180) (other values are correctly wrapped).",
                    allowableValues = "range[-180,180)")
            @PathParam(PARAM_LON_DEG) @Nullable String paramLonDegAsString,
            @ApiParam(hidden = true)
            @QueryParam(PARAM_CLIENT) @DefaultValue("") @Nonnull String paramClient,
            @ApiParam(hidden = true)
            @QueryParam(PARAM_ALLOW_LOG) @DefaultValue("true") @Nonnull String paramAllowLog,
            @Suspended @Nonnull AsyncResponse response) throws ApiException;
```

- [ ] **Step 2: Verify the project still compiles (interface change → impl must implement)**

Run: `mvn -pl service compile -q`
Expected: FAIL — `MapcodeResourceImpl is not abstract and does not override abstract method getTerritoriesForLatLon`. This is intentional: Task 9 implements it. If the compile error is anything else, fix the interface declaration before proceeding.

- [ ] **Step 3: Do NOT commit yet** — the interface change is paired with the impl in the next task. Leave the working tree dirty.

---

### Task 9: Implement the new endpoint in `MapcodeResourceImpl`

**Files:**
- Modify: `service/src/main/java/com/mapcode/services/implementation/MapcodeResourceImpl.java`

- [ ] **Step 1: Add `BoundaryService` as a constructor argument**

Change the constructor in `MapcodeResourceImpl` (lines 87–95) from:

```java
    @Inject
    public MapcodeResourceImpl(
            @Nonnull final ResourceProcessor processor,
            @Nonnull final SystemMetricsCollector metricsCollector) {
        assert processor != null;
        assert metricsCollector != null;
        this.processor = processor;
        this.metricsCollector = metricsCollector;
    }
```

to:

```java
    private final BoundaryService boundaryService;

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

(The new `private final BoundaryService boundaryService;` field goes alongside the existing `processor` and `metricsCollector` fields at lines 60-61. Move it next to them rather than declaring inside the snippet above — show in the final result.)

- [ ] **Step 2: Add the endpoint implementation**

Append this method to `MapcodeResourceImpl` (a good location: just before `convertMapcodeToLatLon` at line 413):

```java
    @Override
    public void getTerritoriesForLatLon(
            @Nullable final String paramLatDegAsString,
            @Nullable final String paramLonDegAsString,
            @Nonnull final String paramClient,
            @Nonnull final String paramAllowLog,
            @Nonnull final AsyncResponse response) throws ApiInvalidFormatException {
        assert response != null;

        processor.process("getTerritoriesForLatLon", LOG, response, () -> {
            LOG.info("getTerritoriesForLatLon: lat={}, lon={}, client={}, allowLog={}",
                    paramLatDegAsString, paramLonDegAsString, paramClient, paramAllowLog);
            metricsCollector.addOneLatLonToTerritoriesRequest(paramClient);

            // Check lat range.
            final double latDeg;
            try {
                latDeg = Double.valueOf(StringUtils.nullToEmpty(paramLatDegAsString));
                if (!MathUtils.isBetween(latDeg, ApiConstants.API_LAT_MIN, ApiConstants.API_LAT_MAX)) {
                    throw new NumberFormatException(paramLatDegAsString);
                }
            } catch (final NumberFormatException e) {
                throw new ApiInvalidFormatException(PARAM_LAT_DEG, paramLatDegAsString,
                        "[" + ApiConstants.API_LAT_MIN + ", " + ApiConstants.API_LAT_MAX + ']');
            }

            // Check lon range (wrapped to [-180, 180]).
            final double lonDeg;
            try {
                lonDeg = Geo.mapToLon(Double.valueOf(StringUtils.nullToEmpty(paramLonDegAsString)));
            } catch (final NumberFormatException e) {
                throw new ApiInvalidFormatException(PARAM_LON_DEG, paramLonDegAsString, "Double");
            }

            final List<TerritoryMatch> matches = boundaryService.lookup(latDeg, lonDeg);
            final List<TerritoryCandidateDTO> candidates = matches.stream()
                    .map(m -> new TerritoryCandidateDTO(m.getAlphaCode(), m.getParentAlphaCode()))
                    .collect(Collectors.toList());
            final TerritoryCandidateListDTO result = new TerritoryCandidateListDTO(candidates);
            result.validate();

            metricsCollector.addOneValidLatLonToTerritoriesRequest(paramClient);
            response.resume(Response.ok(result).build());
            return Futures.successful(null);
        });
    }
```

- [ ] **Step 3: Verify the project compiles**

Run: `mvn -pl service compile -q`
Expected: build succeeds. `LocalTestServer` still compiles because it isn't touched yet (constructor change there happens in Task 11), but the production code is consistent.

Note: if the project compiles `test-compile` too, `LocalTestServer` will fail because of the constructor signature change. That's intentional — fixed in Task 11. For now run only `compile`, not `test-compile`.

- [ ] **Step 4: Commit interface + impl together**

```bash
git add service/src/main/java/com/mapcode/services/MapcodeResource.java \
        service/src/main/java/com/mapcode/services/implementation/MapcodeResourceImpl.java
git commit -m "Add GET /codes/{lat},{lon}/territories endpoint"
```

---

### Task 10: Wire `BoundaryService` as an eager singleton in `ResourcesModule`

**Files:**
- Modify: `service/src/main/java/com/mapcode/services/ResourcesModule.java`

- [ ] **Step 1: Bind `BoundaryService` as an eagerly-constructed instance**

In `service/src/main/java/com/mapcode/services/ResourcesModule.java`, add the import:

```java
import com.mapcode.services.implementation.BoundaryService;
```

In the `configure` method (around line 45-57), append at the end:

```java
        // Construct BoundaryService eagerly so startup fails fast if the
        // borders file is missing or unreadable. Using toInstance(...) here
        // (rather than a @Provides + asEagerSingleton combo) avoids
        // duplicate-binding errors and runs the constructor during Guice
        // configuration, which is exactly when we want to surface a missing
        // borders file.
        binder.bind(BoundaryService.class).toInstance(createBoundaryService());
```

Then add this private helper method to the class (alongside the existing `@Provides` methods):

```java
    @Nonnull
    private static BoundaryService createBoundaryService() {
        final String path = System.getProperty("mapcode.borders.path",
                System.getenv("MAPCODE_BORDERS_PATH"));
        if (path == null || path.isEmpty()) {
            throw new IllegalStateException(
                    "mapcode.borders.path system property or MAPCODE_BORDERS_PATH env var must be set");
        }
        return new BoundaryService(path);
    }
```

- [ ] **Step 2: Verify the project compiles**

Run: `mvn -pl service compile -q`
Expected: build succeeds.

- [ ] **Step 3: Commit**

```bash
git add service/src/main/java/com/mapcode/services/ResourcesModule.java
git commit -m "Bind BoundaryService as eager singleton, fail fast on missing borders file"
```

---

### Task 11: Update `LocalTestServer` to construct `BoundaryService` from the fixture

**Files:**
- Modify: `service/src/test/java/com/mapcode/services/LocalTestServer.java`

- [ ] **Step 1: Wire the fixture into the test server**

In `service/src/test/java/com/mapcode/services/LocalTestServer.java`, modify the `start()` method.

After the `final SystemMetricsImpl metrics = new SystemMetricsImpl();` line (around line 73), add:

```java
        // Borders fixture for territory-lookup tests.
        final String bordersPath = java.nio.file.Paths.get(
                "src", "test", "resources", "borders-test.fgb").toAbsolutePath().toString();
        final com.mapcode.services.implementation.BoundaryService boundaryService =
                new com.mapcode.services.implementation.BoundaryService(bordersPath);
```

Change the `MapcodeResourceImpl` construction (lines 76-79) from:

```java
        final MapcodeResourceImpl mapcodeResource = new MapcodeResourceImpl(
                resourceProcessor,
                metrics
        );
```

to:

```java
        final MapcodeResourceImpl mapcodeResource = new MapcodeResourceImpl(
                resourceProcessor,
                metrics,
                boundaryService
        );
```

- [ ] **Step 2: Verify all existing tests still run**

Run: `mvn -pl service test -q`
Expected: All previously-passing tests still pass. The new `BoundaryServiceTest` continues to pass. No new failures.

- [ ] **Step 3: Commit**

```bash
git add service/src/test/java/com/mapcode/services/LocalTestServer.java
git commit -m "Wire test fixture borders file into LocalTestServer"
```

---

### Task 12: End-to-end HTTP tests for the new endpoint

**Files:**
- Create: `service/src/test/java/com/mapcode/services/ApiCodesTerritoriesTest.java`

- [ ] **Step 1: Write the integration tests**

Create `service/src/test/java/com/mapcode/services/ApiCodesTerritoriesTest.java`:

```java
/*
 * Copyright (C) 2016-2026, Stichting Mapcode Foundation (http://www.mapcode.com)
 */
package com.mapcode.services;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class ApiCodesTerritoriesTest {
    private static final Logger LOG = LoggerFactory.getLogger(ApiCodesTerritoriesTest.class);

    private LocalTestServer server;

    @Before
    public void startServer() {
        server = new LocalTestServer("1.0", 8081);
        server.start();
    }

    @After
    public void stopServer() {
        server.stop();
    }

    @Test
    public void pointInsideCountryReturnsCountryJson() {
        // (52, 5) is inside the NLD fixture polygon.
        final Response response = new ResteasyClientBuilder().build()
                .target(server.url("/mapcode/codes/52.0,5.0/territories"))
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(
                "{\"territories\":[{\"alphaCode\":\"NLD\"}]}",
                response.readEntity(String.class));
    }

    @Test
    public void pointInsideSubdivisionReturnsSubdivisionThenCountryJson() {
        // (36, -120) is inside the USA-CA fixture polygon, which is inside USA.
        final Response response = new ResteasyClientBuilder().build()
                .target(server.url("/mapcode/codes/36.0,-120.0/territories"))
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(
                "{\"territories\":[" +
                        "{\"alphaCode\":\"USA-CA\",\"parentAlphaCode\":\"USA\"}," +
                        "{\"alphaCode\":\"USA\"}]}",
                response.readEntity(String.class));
    }

    @Test
    public void pointAtSeaReturnsEmptyListJson() {
        // (0, -30) mid-Atlantic — no fixture polygon contains it.
        final Response response = new ResteasyClientBuilder().build()
                .target(server.url("/mapcode/codes/0.0,-30.0/territories"))
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertEquals(200, response.getStatus());
        // Empty list serializes to no `territories` key because of NON_EMPTY include.
        Assert.assertEquals("{}", response.readEntity(String.class));
    }

    @Test
    public void disputedRegionReturnsSmallerPolygonFirstJson() {
        // (6.5, 106.5) is inside both DISPUTED-A and DISPUTED-B; B is smaller.
        final Response response = new ResteasyClientBuilder().build()
                .target(server.url("/mapcode/codes/6.5,106.5/territories"))
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(
                "{\"territories\":[" +
                        "{\"alphaCode\":\"DISPUTED-B\"}," +
                        "{\"alphaCode\":\"DISPUTED-A\"}]}",
                response.readEntity(String.class));
    }

    @Test
    public void latOutOfRangeReturns400() {
        final Response response = new ResteasyClientBuilder().build()
                .target(server.url("/mapcode/codes/91.0,5.0/territories"))
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertEquals(400, response.getStatus());
    }

    @Test
    public void lonOutOfRangeIsWrapped() {
        // 360+5 == 5 modulo wrap → should behave like lon=5.0 → inside NLD.
        final Response response = new ResteasyClientBuilder().build()
                .target(server.url("/mapcode/codes/52.0,365.0/territories"))
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(
                "{\"territories\":[{\"alphaCode\":\"NLD\"}]}",
                response.readEntity(String.class));
    }

    @Test
    public void pointInsideCountryReturnsCountryXml() {
        final Response response = new ResteasyClientBuilder().build()
                .target(server.url("/mapcode/codes/52.0,5.0/territories"))
                .request()
                .accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                        "<territories><territoryCandidate><alphaCode>NLD</alphaCode>" +
                        "</territoryCandidate></territories>",
                response.readEntity(String.class));
    }
}
```

- [ ] **Step 2: Run the new tests**

Run: `mvn -pl service test -Dtest=ApiCodesTerritoriesTest -q`
Expected: all 7 tests PASS.

> If the empty-list JSON serialization comes back differently from `{}` (e.g., `{"territories":[]}`), that's because Jackson's `Include.NON_EMPTY` may not strip empty lists in all configurations. Adjust the expected string in `pointAtSeaReturnsEmptyListJson` to whatever the framework actually produces — the contract from the spec is "an empty list", whichever serialization that yields. Other tests should not need adjustment.

- [ ] **Step 3: Commit**

```bash
git add service/src/test/java/com/mapcode/services/ApiCodesTerritoriesTest.java
git commit -m "Add end-to-end HTTP tests for territory lookup endpoint"
```

---

### Task 13: Update the help page in `RootResourceImpl`

**Files:**
- Modify: `service/src/main/java/com/mapcode/services/implementation/RootResourceImpl.java`

- [ ] **Step 1: Insert the new route into the `HELP_TEXT` literal**

In `RootResourceImpl.HELP_TEXT`, insert the following lines after the existing `GET /mapcode/codes/{lat},{lon}...` block (after the `include` parameter documentation around line 113, before the `GET /mapcode/coords/{code}` line at 115):

```java
            "GET /mapcode/codes/{lat},{lon}/territories\n" +
            "   Look up the ranked list of mapcode territories containing a lat/lon. Backed by OSM\n" +
            "   admin-boundary data. Most specific territory first (subdivision before country),\n" +
            "   with smaller polygons ranked before larger ones at the same admin level.\n\n" +

            "   Path parameters:\n" +
            "     lat             : Latitude, range [-90, 90] (automatically limited to this range).\n" +
            "     lon             : Longitude, range [-180, 180] (automatically wrapped to this range).\n\n" +

            "   Returns: an object with a `territories` array of `{alphaCode, parentAlphaCode?}` entries.\n" +
            "   Empty list when no admin polygon contains the point (e.g., at sea).\n\n" +
```

- [ ] **Step 2: Verify the project builds and tests pass**

Run: `mvn -pl service test -q`
Expected: All tests pass (no test depends on the exact help-text length).

- [ ] **Step 3: Commit**

```bash
git add service/src/main/java/com/mapcode/services/implementation/RootResourceImpl.java
git commit -m "Document /codes/{lat},{lon}/territories in help page"
```

---

### Task 14: Create the production `build-borders.py` script

**Files:**
- Create: `tools/build-borders.py`

This is an operator-facing script that runs out-of-band (Q13). It is not exercised by the test suite, but it must exist and be runnable.

- [ ] **Step 1: Write the script skeleton**

Create `tools/build-borders.py`:

```python
#!/usr/bin/env python3
"""Build the production borders FlatGeobuf from OSM admin-boundary data.

Pipeline:
  1. Read OSM admin_level 2 (countries) and admin_level 4 (first-level
     subdivisions) polygons from the input source.
  2. Resolve each polygon's mapcode alphaCode from its ISO 3166 tags:
       - admin_level 2 -> ISO3166-1:alpha3, looked up against the
         iso-to-mapcode table.
       - admin_level 4 -> ISO3166-2 (e.g., "US-CA"); if no mapcode
         equivalent exists, fall back to the parent country's
         alphaCode. If neither, drop the polygon.
  3. Simplify each polygon with Douglas-Peucker (tolerance ~50 m, i.e.
     ~0.00045 deg at the equator; configurable via --tolerance).
  4. Compute polygon area (square degrees is fine; only used as a tie-
     breaker, not displayed).
  5. Write FlatGeobuf with feature properties:
       alphaCode (string), parentAlphaCode (string, "" when absent),
       adminLevel (int), area (double).

The iso-to-mapcode mapping is loaded from --mapping (a JSON file). To
regenerate that file from the mapcode library, run:
    mvn -pl service exec:java \\
        -Dexec.mainClass="com.mapcode.tools.ExportIsoToMapcode" \\
        -Dexec.args="tools/iso-to-mapcode.json"
(This Maven exec target is not part of this script. Operators can write
a small Java helper or maintain the JSON by hand for now.)

Run:
    python3 tools/build-borders.py \\
        --osm <osm.pbf-or-geojson> \\
        --mapping tools/iso-to-mapcode.json \\
        --out borders.fgb \\
        [--tolerance 0.00045]
"""
import argparse
import json
import sys
from pathlib import Path

import geopandas as gpd
from flatgeobuf import FlatGeobufWriter
from shapely.geometry import mapping


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description=__doc__,
                                formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--osm", required=True,
                   help="Path to an OSM extract (e.g., .osm.pbf or GeoJSON) "
                        "with admin_level 2 and 4 boundary polygons.")
    p.add_argument("--mapping", required=True,
                   help="Path to a JSON file mapping ISO codes to mapcode "
                        "alphaCodes. Format: {\"USA\": {\"alphaCode\": \"USA\"}, "
                        "\"US-CA\": {\"alphaCode\": \"USA-CA\", "
                        "\"parentAlphaCode\": \"USA\"}, ...}")
    p.add_argument("--out", required=True,
                   help="Output FlatGeobuf path.")
    p.add_argument("--tolerance", type=float, default=0.00045,
                   help="Douglas-Peucker simplification tolerance, degrees. "
                        "Default ~50 m at the equator.")
    return p.parse_args()


def load_mapping(path: Path) -> dict:
    with open(path) as f:
        return json.load(f)


def resolve_alpha_code(iso: str, admin_level: int, mapping: dict):
    """Return (alphaCode, parentAlphaCode) or None to drop the polygon.

    For admin_level 4: try the ISO-3166-2 code; if missing in the mapping,
    fall back to the parent country's mapcode alphaCode (derived from the
    leading characters of the ISO-3166-2 code). If neither exists, return
    None.
    """
    entry = mapping.get(iso)
    if entry is not None:
        return entry["alphaCode"], entry.get("parentAlphaCode")
    if admin_level == 4 and "-" in iso:
        country = iso.split("-", 1)[0]
        parent_entry = mapping.get(country)
        if parent_entry is not None:
            return parent_entry["alphaCode"], None
    return None


def main() -> int:
    args = parse_args()
    mapping = load_mapping(Path(args.mapping))

    gdf = gpd.read_file(args.osm)
    gdf = gdf[gdf["admin_level"].isin([2, 4])]

    written = dropped = 0
    with FlatGeobufWriter(
        args.out,
        geometry_type="Polygon",
        properties={
            "alphaCode": "String",
            "parentAlphaCode": "String",
            "adminLevel": "Int",
            "area": "Double",
        },
    ) as w:
        for _, row in gdf.iterrows():
            level = int(row["admin_level"])
            iso = row.get("ISO3166-1:alpha3") if level == 2 else row.get("ISO3166-2")
            if iso is None:
                dropped += 1
                continue
            resolved = resolve_alpha_code(iso, level, mapping)
            if resolved is None:
                dropped += 1
                continue
            alpha, parent = resolved
            geom = row.geometry.simplify(args.tolerance, preserve_topology=True)
            if geom.is_empty:
                dropped += 1
                continue
            w.add(
                geometry=mapping_geom(geom),
                properties={
                    "alphaCode": alpha,
                    "parentAlphaCode": parent or "",
                    "adminLevel": level,
                    "area": geom.area,
                },
            )
            written += 1

    print(f"Wrote {written} polygons to {args.out}; dropped {dropped}.")
    return 0


def mapping_geom(geom):
    # Wrapper kept separate from shapely.mapping in case Polygon vs MultiPolygon
    # ever need different handling.
    return mapping(geom)


if __name__ == "__main__":
    sys.exit(main())
```

- [ ] **Step 2: Verify the script at least parses**

Run: `python3 -m py_compile tools/build-borders.py`
Expected: no output, exit 0.

(We deliberately do not require running the script end-to-end here. Operators run it with real data when refreshing the dataset.)

- [ ] **Step 3: Commit**

```bash
git add tools/build-borders.py
git commit -m "Add production build-borders.py script"
```

---

## Final verification

After all tasks: run the full test suite and confirm everything is green.

- [ ] **Run all tests**

Run: `mvn -pl service test -q`
Expected: all tests pass, including the existing test classes (`ApiTerritoriesTest`, `ApiCodesTest`, etc.) and the three new ones (`TerritoryCandidateDTOTest`, `TerritoryCandidateListDTOTest`, `BoundaryServiceTest`, `ApiCodesTerritoriesTest`).

- [ ] **Sanity check: GitNexus index refresh per CLAUDE.md**

After committing code changes:

```bash
npx gitnexus analyze
```

(If you previously had embeddings, use `npx gitnexus analyze --embeddings`. Inspect `.gitnexus/meta.json`'s `stats.embeddings` field to know which case applies.)

- [ ] **Sanity check: impact analysis on touched symbols**

Per CLAUDE.md, run impact analysis at the *start* of each task involving modifications to existing symbols (this is each task touching `MapcodeResource`, `MapcodeResourceImpl`, `SystemMetricsCollector`, `SystemMetrics`, `SystemMetricsImpl`, `ResourcesModule`, `RootResourceImpl`, `LocalTestServer`). Worker agents executing this plan should:

```
gitnexus_impact({target: "<symbol>", direction: "upstream"})
```

before editing. If HIGH/CRITICAL risk surfaces unexpectedly, stop and report.

---

## Out of scope (per spec)

- Optional `?bufferMeters=…` for neighbor candidates near borders.
- Editorial overrides for disputed regions.
- Hot reload of the borders file.
- admin_level 6 (counties).
- An automated `mvn` target to generate `tools/iso-to-mapcode.json` from the mapcode library — the `build-borders.py` script accepts the JSON as input; operators maintain the mapping out-of-band for now.
