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
        super();
    }
}
