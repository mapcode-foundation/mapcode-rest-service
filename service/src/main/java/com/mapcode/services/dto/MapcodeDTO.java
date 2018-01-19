/*
 * Copyright (C) 2016-2018, Stichting Mapcode Foundation (http://www.mapcode.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@SuppressWarnings({"NonFinalFieldReferenceInEquals", "NonFinalFieldReferencedInHashCode", "NullableProblems", "EqualsWhichDoesntCheckParameterClass"})
@ApiModel(
        value = "mapcode",
        description = "A mapcode object, such as returned by `GET /mapcode/codes/52,5/local`.")
@JsonInclude(Include.NON_EMPTY)
@XmlRootElement(name = "mapcode")
@XmlAccessorType(XmlAccessType.FIELD)
public final class MapcodeDTO extends ApiDTO {

    @ApiModelProperty(
            name = "mapcode",
            value = "The mapcode without the territory code. Format: 5-10 characters, including the '.'.",
            example = "2TNM")
    @XmlElement(name = "mapcode")
    @Nonnull
    private String mapcode;

    @ApiModelProperty(
            name = "mapcodeInAlphabet",
            value = "(optional) The same mapcode in a specific alphabet. " +
                    "To view which alphabets are available: `GET /mapcode/alphabets`")
    @XmlElement(name = "mapcodeInAlphabet")
    @Nullable
    private String mapcodeInAlphabet;

    @ApiModelProperty(
            name = "territory",
            value = "The territory code. Format: `XXX` or `XX-YY`. " +
                    "To view which alphabets are available: `GET /mapcode/territories`",
            example = "NLD")
    @XmlElement(name = "territory")
    @Nullable
    private String territory;

    @ApiModelProperty(
            name = "territoryInAlphabet",
            value = "(optional) The same territory code in a specific alphabet.")
    @XmlElement(name = "territoryInAlphabet")
    @Nullable
    private String territoryInAlphabet;

    @ApiModelProperty(
            name = "offsetMeters",
            value = "(optional) The approximated offset from the specified coordinate " +
                    "to the center of this mapcode area.")
    @XmlElement(name = "offsetMeters")
    @Nullable
    private Double offsetMeters;

    @ApiModelProperty(
            name = "rectangle",
            value = "(optional) The rectangular area covered by the mapcode.")
    @XmlElement(name = "rectangle")
    @Nullable
    private RectangleDTO rectangle;

    @Override
    public void validate() {
        validator().start();
        validator().checkString(true, "mapcode", mapcode, ApiConstants.API_MAPCODE_LEN_MIN, ApiConstants.API_MAPCODE_LEN_MAX);
        validator().checkString(false, "mapcodeInAlphabet", mapcodeInAlphabet, ApiConstants.API_MAPCODE_LEN_MIN, ApiConstants.API_MAPCODE_LEN_MAX);
        validator().checkString(false, "territory", territory, ApiConstants.API_TERRITORY_LEN_MIN, ApiConstants.API_TERRITORY_LEN_MAX);
        validator().checkString(false, "territoryInAlphabet", territoryInAlphabet, ApiConstants.API_TERRITORY_LEN_MIN, ApiConstants.API_TERRITORY_LEN_MAX);
        validator().checkDouble(false, "offsetMeters", offsetMeters, -Double.MAX_VALUE, Double.MAX_VALUE, false);
        validator().checkNotNullAndValidate(false, "rectangle", rectangle);
        validator().done();
    }

    public MapcodeDTO(
            @Nonnull final String mapcode,
            @Nullable final String mapcodeInAlphabet,
            @Nullable final String territory,
            @Nullable final String territoryInAlphabet,
            @Nullable final Double offsetMeters,
            @Nullable final RectangleDTO rectangle) {
        this.mapcode = mapcode;
        this.mapcodeInAlphabet = mapcodeInAlphabet;
        this.territory = territory;
        this.territoryInAlphabet = territoryInAlphabet;
        this.offsetMeters = offsetMeters;
        this.rectangle = rectangle;
    }

    public MapcodeDTO(
            @Nonnull final String mapcode,
            @Nullable final String mapcodeInAlphabet,
            @Nullable final String territory,
            @Nullable final String territoryInAlphabet,
            @Nullable final Double offsetMeters) {
        this(mapcode, mapcodeInAlphabet, territory, territoryInAlphabet, offsetMeters, null);
    }

    public MapcodeDTO(
            @Nonnull final String mapcode,
            @Nullable final String mapcodeInAlphabet,
            @Nullable final String territory,
            @Nullable final String territoryInAlphabet) {
        this(mapcode, mapcodeInAlphabet, territory, territoryInAlphabet, null, null);
    }

    public MapcodeDTO(
            @Nonnull final String mapcode,
            @Nullable final String mapcodeInAlphabet,
            @Nullable final String territory) {
        this(mapcode, mapcodeInAlphabet, territory, null, null, null);
    }

    public MapcodeDTO(
            @Nonnull final String mapcode,
            @Nullable final String mapcodeInAlphabet) {
        this(mapcode, mapcodeInAlphabet, null, null, null, null);
    }

    public MapcodeDTO(@Nonnull final String mapcode) {
        this(mapcode, null, null, null, null, null);
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

    @Nullable
    public RectangleDTO getRectangle() {
        beforeGet();
        return rectangle;
    }

    public void setRectangle(@Nullable final RectangleDTO rectangle) {
        beforeSet();
        this.rectangle = rectangle;
    }
}
