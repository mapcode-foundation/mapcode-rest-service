/*
 * Copyright (C) 2016-2017, Stichting Mapcode Foundation (http://www.mapcode.com)
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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tomtom.speedtools.apivalidation.ApiDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.annotation.*;
import java.util.List;

@SuppressWarnings({"NonFinalFieldReferenceInEquals", "NonFinalFieldReferencedInHashCode", "NullableProblems", "EqualsWhichDoesntCheckParameterClass"})
@ApiModel(
        value = "mapcodes",
        description = "A full coordinate to mapcode response object, such as returned by `GET /mapcode/codes/52,5`.")
@JsonInclude(Include.NON_EMPTY)
@XmlRootElement(name = "mapcodes")
@XmlAccessorType(XmlAccessType.FIELD)
public final class MapcodesDTO extends ApiDTO {

    @ApiModelProperty(
            name = "local",
            value = "A local mapcode. This is the shortest local mapcode which seems to best match input coordinate. " +
                    "Note that coordinates near borders of adjacent territories may be covered by different local " +
                    "mapcodes (with different territory codes). In such cases, the 'local' mapcode may not always " +
                    "specify the territory you would expect. The `mapcodes` attribute will contain the 'correct' local " +
                    "mapcode in those cases. This `local` mapcode is only offered as a convenience.")
    @XmlElement(name = "local")
    @Nullable
    private MapcodeDTO local;

    @ApiModelProperty(
            name = "international",
            value = "The international mapcode. This is globally unique mapcode, which does not require a territory " +
                    "code. The downside of using international mapcodes is their length: they are always 10 characters.")
    @XmlElement(name = "international")
    @Nonnull
    private MapcodeDTO international;

    @ApiModelProperty(
            name = "mapcodes",
            value = "The list of all alternative mapcodes for the specified coordinate. Coordinates near borders of " +
                    "territories may be covered by mapcodes from multiple territories and within a single territory, " +
                    "mapcodes of different lengths may exist. Normally, the logical thing to do, is select the shortest " +
                    "mapcode in the correct territory from this list. The attribute `local` tries to achieve this as well " +
                    "but in some cases it may use a territory you don't wish to use.",
            dataType = "com.mapcode.services.dto.MapcodeDTO",
            reference = "com.mapcode.services.dto.MapcodeDTO")
    @JsonProperty("mapcodes")
    @XmlElementWrapper(name = "mapcodes")
    @XmlElement(name = "mapcode")
    @Nonnull
    private MapcodeListDTO mapcodes;

    @Override
    public void validate() {
        validator().start();
        validator().checkNotNullAndValidate(false, "local", local);
        validator().checkNotNullAndValidate(true, "international", international);
        validator().checkNotNullAndValidateAll(true, "mapcodes", mapcodes);
        validator().done();
    }

    public MapcodesDTO(
            @Nullable final MapcodeDTO local,
            @Nonnull final MapcodeDTO international,
            @Nonnull final MapcodeListDTO mapcodes) {
        this.local = local;
        this.international = international;
        this.mapcodes = mapcodes;
    }

    public MapcodesDTO(
            @Nullable final MapcodeDTO local,
            @Nonnull final MapcodeDTO international,
            @Nonnull final List<MapcodeDTO> mapcodes) {
        this(local, international, new MapcodeListDTO(mapcodes));
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    private MapcodesDTO() {
        // Default constructor required by JAX-B.
        super();
    }

    @Nullable
    public MapcodeDTO getLocal() {
        beforeGet();
        return local;
    }

    public void setLocal(@Nullable final MapcodeDTO local) {
        beforeSet();
        this.local = local;
    }

    @Nonnull
    public MapcodeDTO getInternational() {
        beforeGet();
        return international;
    }

    public void setInternational(@Nonnull final MapcodeDTO international) {
        beforeSet();
        assert international != null;
        this.international = international;
    }

    @Nonnull
    public List<MapcodeDTO> getMapcodes() {
        beforeGet();
        return mapcodes;
    }

    public void setMapcodes(@Nonnull final MapcodeListDTO mapcodes) {
        beforeSet();
        assert mapcodes != null;
        this.mapcodes = mapcodes;
    }
}
