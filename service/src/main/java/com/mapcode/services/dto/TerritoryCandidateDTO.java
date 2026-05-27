/*
 * Copyright (C) 2016-2026, Stichting Mapcode Foundation (http://www.mapcode.com)
 */
package com.mapcode.services.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.mapcode.services.ApiConstants;
import com.tomtom.speedtools.apivalidation.ApiDTO;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.annotation.*;

@SuppressWarnings({"NullableProblems", "InstanceVariableMayNotBeInitialized"})
@JsonInclude(Include.NON_EMPTY)
@XmlRootElement(name = "territoryCandidate")
@XmlAccessorType(XmlAccessType.FIELD)
public final class TerritoryCandidateDTO extends ApiDTO {

    @XmlElement(name = "alphaCode")
    @Nonnull
    private String alphaCode;

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
