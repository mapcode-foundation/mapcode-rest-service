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
