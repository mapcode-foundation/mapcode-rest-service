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

import com.tomtom.speedtools.apivalidation.ApiDataBinder;

import javax.annotation.Nonnull;
import java.util.List;

@SuppressWarnings({"NonFinalFieldReferenceInEquals", "NonFinalFieldReferencedInHashCode"})
public final class MapcodesBinder extends ApiDataBinder {

    @Nonnull
    private List<MapcodeBinder> mapcodes;

    @Override
    public void validate() {
        validator().start();
        validator().checkNotNull(true, "mapcodes", mapcodes);
        validator().done();
    }

    public MapcodesBinder(@Nonnull final List<MapcodeBinder> mapcodes) {
        this.mapcodes = mapcodes;
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    private MapcodesBinder() {
        // Default constructor required by JAX-B.
        super();
    }

    @Nonnull
    public List<MapcodeBinder> getMapcodes() {
        beforeGet();
        return mapcodes;
    }

    public void setMapcode(@Nonnull final List<MapcodeBinder> mapcodes) {
        beforeSet();
        assert mapcodes != null;
        this.mapcodes = mapcodes;
    }

}
