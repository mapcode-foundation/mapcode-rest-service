/*
 * Copyright (C) 2015 Stichting Mapcode Foundation (http://www.mapcode.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mapcode.services.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.mapcode.services.ApiConstants;
import com.tomtom.speedtools.apivalidation.ApiDTO;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@SuppressWarnings({"NonFinalFieldReferenceInEquals", "NonFinalFieldReferencedInHashCode", "NullableProblems", "EqualsWhichDoesntCheckParameterClass"})
@JsonInclude(Include.NON_EMPTY)
@XmlRootElement(name = "mapcode")
@XmlAccessorType(XmlAccessType.FIELD)
public final class MapcodeDTO extends ApiDTO {

    @Nonnull
    private String mapcode;

    @Nullable
    private String mapcodeInAlphabet;

    @Nullable
    private String territory;

    @Nullable
    private String territoryInAlphabet;

    @Nullable
    private Double offsetMeters;

    @Override
    public void validate() {
        validator().start();
        validator().checkString(true, "mapcode", mapcode, ApiConstants.API_MAPCODE_LEN_MIN, ApiConstants.API_MAPCODE_LEN_MAX);
        validator().checkString(false, "mapcodeInAlphabet", mapcodeInAlphabet, ApiConstants.API_MAPCODE_LEN_MIN, ApiConstants.API_MAPCODE_LEN_MAX);
        validator().checkString(false, "territory", territory, ApiConstants.API_TERRITORY_LEN_MIN, ApiConstants.API_TERRITORY_LEN_MAX);
        validator().checkString(false, "territoryInAlphabet", territoryInAlphabet, ApiConstants.API_TERRITORY_LEN_MIN, ApiConstants.API_TERRITORY_LEN_MAX);
        validator().checkDouble(false, "offsetMeters", offsetMeters, -Double.MAX_VALUE, Double.MAX_VALUE, false);
        validator().done();
    }

    public MapcodeDTO(
            @Nonnull final String mapcode,
            @Nullable final String mapcodeInAlphabet,
            @Nullable final String territory,
            @Nullable final String territoryInAlphabet,
            @Nullable final Double offsetMeters) {
        this.mapcode = mapcode;
        this.mapcodeInAlphabet = mapcodeInAlphabet;
        this.territory = territory;
        this.territoryInAlphabet = territoryInAlphabet;
        this.offsetMeters = offsetMeters;
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    private MapcodeDTO() {
        // Default constructor required by JAX-B.
        super();
    }

    @Nonnull
    public String getMapcode() {
        beforeGet();
        return mapcode;
    }

    public void setMapcode(@Nonnull final String mapcode) {
        beforeSet();
        assert mapcode != null;
        this.mapcode = mapcode;
    }

    @Nullable
    public String getMapcodeInAlphabet() {
        beforeGet();
        return mapcodeInAlphabet;
    }

    public void setMapcodeInAlphabet(@Nullable final String mapcodeInAlphabet) {
        beforeSet();
        this.mapcodeInAlphabet = mapcodeInAlphabet;
    }

    @Nullable
    public String getTerritory() {
        beforeGet();
        return territory;
    }

    public void setTerritory(@Nullable final String territory) {
        beforeSet();
        this.territory = territory;
    }

    @Nullable
    public String getTerritoryInAlphabet() {
        beforeGet();
        return territoryInAlphabet;
    }

    public void setTerritoryInAlphabet(@Nullable final String territoryInAlphabet) {
        beforeSet();
        this.territoryInAlphabet = territoryInAlphabet;
    }

    @Nullable
    public Double getOffsetMeters() {
        beforeGet();
        return offsetMeters;
    }

    public void setOffsetMeters(@Nullable final Double offsetMeters) {
        beforeSet();
        this.offsetMeters = offsetMeters;
    }

    @Override
    @Nonnull
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public boolean equals(@Nullable final Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj, false);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, false);
    }
}
