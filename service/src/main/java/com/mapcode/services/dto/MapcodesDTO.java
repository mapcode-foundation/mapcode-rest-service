/*
 * Copyright (C) 2016-2020, Stichting Mapcode Foundation (http://www.mapcode.com)
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.annotation.*;
import java.util.List;

@SuppressWarnings({"NullableProblems", "InstanceVariableMayNotBeInitialized"})
@JsonInclude(Include.NON_EMPTY)
@XmlRootElement(name = "mapcodes")
@XmlAccessorType(XmlAccessType.FIELD)
public final class MapcodesDTO extends ApiDTO {

    @XmlElement(name = "local")
    @Nullable
    private MapcodeDTO local;

    @XmlElement(name = "international")
    @Nonnull
    private MapcodeDTO international;

    @JsonProperty("mapcodes")
    @XmlElementWrapper(name = "mapcodes")
    @XmlElement(name = "mapcode")
    @Nonnull
    private MapcodeListDTO mapcodes;

    @JsonProperty("territories")
    @XmlElementWrapper(name = "territories")
    @XmlElement(name = "territory")
    @Nullable
    private TerritoryCandidateListDTO territories;

    @Override
    public void validate() {
        validator().start();
        validator().checkNotNullAndValidate(false, "local", local);
        validator().checkNotNullAndValidate(true, "international", international);
        validator().checkNotNullAndValidateAll(true, "mapcodes", mapcodes);
        validator().checkNotNullAndValidateAll(false, "territories", territories);
        validator().done();
    }

    public MapcodesDTO(
            @Nullable final MapcodeDTO local,
            @Nonnull final MapcodeDTO international,
            @Nonnull final MapcodeListDTO mapcodes,
            @Nullable final TerritoryCandidateListDTO territories) {
        this.local = local;
        this.international = international;
        this.mapcodes = mapcodes;
        this.territories = territories;
    }

    public MapcodesDTO(
            @Nullable final MapcodeDTO local,
            @Nonnull final MapcodeDTO international,
            @Nonnull final MapcodeListDTO mapcodes) {
        this(local, international, mapcodes, null);
    }

    public MapcodesDTO(
            @Nullable final MapcodeDTO local,
            @Nonnull final MapcodeDTO international,
            @Nonnull final List<MapcodeDTO> mapcodes) {
        this(local, international, new MapcodeListDTO(mapcodes), null);
    }

    public MapcodesDTO(
            @Nullable final MapcodeDTO local,
            @Nonnull final MapcodeDTO international,
            @Nonnull final List<MapcodeDTO> mapcodes,
            @Nullable final List<TerritoryCandidateDTO> territories) {
        this(local, international, new MapcodeListDTO(mapcodes),
                (territories == null) ? null : new TerritoryCandidateListDTO(territories));
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

    @Nullable
    public List<TerritoryCandidateDTO> getTerritories() {
        beforeGet();
        return territories;
    }

    public void setTerritories(@Nullable final TerritoryCandidateListDTO territories) {
        beforeSet();
        this.territories = territories;
    }
}
