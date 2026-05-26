/*
 * Copyright (C) 2016-2026, Stichting Mapcode Foundation (http://www.mapcode.com)
 */
package com.mapcode.services.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.tomtom.speedtools.apivalidation.ApiDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@SuppressWarnings({"NullableProblems", "InstanceVariableMayNotBeInitialized"})
@ApiModel(
        value = "territoryCandidates",
        description = "Ranked list of territory candidates for a lat/lon, as returned by " +
                "`GET /mapcode/codes/{lat},{lon}/territories`.")
@JsonInclude(Include.NON_EMPTY)
@XmlRootElement(name = "territories")
@XmlAccessorType(XmlAccessType.FIELD)
public final class TerritoryCandidatesDTO extends ApiDTO {

    @ApiModelProperty(
            name = "territories",
            value = "Ranked list of territory candidates; most specific first.",
            dataType = "com.mapcode.services.dto.TerritoryCandidateListDTO",
            reference = "com.mapcode.services.dto.TerritoryCandidateListDTO")
    @JsonProperty("territories")
    @JsonUnwrapped
    @XmlElement(name = "territoryCandidate")
    @Nonnull
    private TerritoryCandidateListDTO territories;

    @Override
    public void validate() {
        validator().start();
        validator().checkNotNullAndValidateAll(false, "territories", territories);
        validator().done();
    }

    public TerritoryCandidatesDTO(@Nonnull final TerritoryCandidateListDTO territories) {
        this.territories = territories;
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    private TerritoryCandidatesDTO() {
        // Default constructor required by JAX-B.
        super();
    }

    @Nonnull
    public TerritoryCandidateListDTO getTerritories() {
        beforeGet();
        return territories;
    }

    public void setTerritories(@Nonnull final TerritoryCandidateListDTO territories) {
        beforeSet();
        this.territories = territories;
    }
}
