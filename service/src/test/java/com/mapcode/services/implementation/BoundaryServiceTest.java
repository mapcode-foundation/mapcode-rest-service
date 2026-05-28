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
    public void subdivisionPreTaggedWithCountryCodeIsReturnedTwice() {
        // Point (62, 22.5) is inside both NO-MAPCODE-PARENT polygons (admin 2 and 4).
        // build-borders.py pre-tags subdivisions that have no mapcode equivalent with the
        // parent country's alphaCode at build time; there is no runtime fallback.
        // This test only verifies that the loader reads the pre-tagged alphaCode correctly.
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
            // After repeated lookups across many polygons, only one prepared
            // geometry should remain cached.
            assertEquals(1, svc.preparedCacheSize());
        } finally {
            System.clearProperty("mapcode.boundary.prepared-cache-size");
        }
    }

    @Test
    public void invalidPreparedCacheSizeFallsBackInsteadOfCrashing() {
        // Non-numeric and non-positive values must not break construction.
        for (final String bad : new String[] {"not-a-number", "0", "-5"}) {
            System.setProperty("mapcode.boundary.prepared-cache-size", bad);
            try {
                final BoundaryService svc = new BoundaryService(FIXTURE.toString());
                assertEquals("NLD", svc.lookup(52.0, 5.0).get(0).getAlphaCode());
            } finally {
                System.clearProperty("mapcode.boundary.prepared-cache-size");
            }
        }
    }
}
