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

package com.mapcode.services.binders;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.mapcode.Territory;
import com.mapcode.services.ApiConstants;
import com.tomtom.speedtools.apivalidation.ApiDataBinder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@SuppressWarnings({"NonFinalFieldReferenceInEquals", "NonFinalFieldReferencedInHashCode"})
@JsonInclude(Include.NON_EMPTY)
public final class MapcodeBinder extends ApiDataBinder {

    @Nonnull
    private String mapcode;

    @Nonnull
    private Territory territory;

    @Nullable
    private Double offsetMeters;

    @Override
    public void validate() {
        validator().start();
        validator().checkString(true, "mapcode", mapcode,
                ApiConstants.API_MAPCODE_LEN_MIN,
                ApiConstants.API_MAPCODE_LEN_MAX);
        validator().checkNotNullAndValidateEnum(true, "territory", territory);
        validator().checkDouble(false, "offsetMeters", offsetMeters, Double.MIN_VALUE, Double.MAX_VALUE, false);
        validator().done();
    }

    public MapcodeBinder(
            @Nonnull final String mapcode,
            @Nonnull final Territory territory,
            @Nullable final Double offsetMeters) {
        this.mapcode = mapcode;
        this.territory = territory;
        this.offsetMeters = offsetMeters;
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    private MapcodeBinder() {
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

    @Nonnull
    public Territory getTerritory() {
        beforeGet();
        return territory;
    }

    public void setTerritory(@Nonnull final Territory territory) {
        beforeSet();
        assert territory != null;
        this.territory = territory;
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
}
