/*
 * Copyright (C) 2016-2026, Stichting Mapcode Foundation (http://www.mapcode.com)
 */
package com.mapcode.services.implementation;

import org.junit.Test;

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
        // (6.5, 106.5) is inside both DISPUTED-A (large) and DISPUTED-B (small).
        final List<TerritoryMatch> matches = svc.lookup(6.5, 106.5);
        assertEquals(2, matches.size());
        assertEquals("DISPUTED-B", matches.get(0).getAlphaCode());
        assertEquals("DISPUTED-A", matches.get(1).getAlphaCode());
    }

    @Test
    public void subdivisionCollapsedToCountryAppearsInBothEntries() {
        // Point (62, 22.5) is inside both NO-MAPCODE-PARENT polygons (admin 2 and 4).
        // The fixture simulates the build-time fallback: the subdivision is tagged with
        // the parent country's alphaCode. The runtime returns both entries, smaller first.
        final BoundaryService svc = new BoundaryService(FIXTURE.toString());
        final List<TerritoryMatch> matches = svc.lookup(62.0, 22.5);
        assertEquals(2, matches.size());
        assertEquals("NO-MAPCODE-PARENT", matches.get(0).getAlphaCode());
        assertEquals(4, matches.get(0).getAdminLevel());
        assertEquals("NO-MAPCODE-PARENT", matches.get(1).getAlphaCode());
        assertEquals(2, matches.get(1).getAdminLevel());
    }

    @Test(expected = IllegalStateException.class)
    public void missingFileFailsConstruction() {
        new BoundaryService("/does/not/exist.fgb");
    }
}
