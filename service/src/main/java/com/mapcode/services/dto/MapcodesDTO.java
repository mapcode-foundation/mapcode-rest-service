/*
 * Copyright (C) 2016, Stichting Mapcode Foundation (http://www.mapcode.com)
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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tomtom.speedtools.apivalidation.ApiDTO;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@SuppressWarnings({"NonFinalFieldReferenceInEquals", "NonFinalFieldReferencedInHashCode", "NullableProblems", "EqualsWhichDoesntCheckParameterClass"})
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
