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
