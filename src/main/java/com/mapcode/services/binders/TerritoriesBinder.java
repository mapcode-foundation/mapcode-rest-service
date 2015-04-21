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
import com.tomtom.speedtools.apivalidation.ApiDataBinder;

import javax.annotation.Nonnull;
import java.util.List;

@SuppressWarnings({"NonFinalFieldReferenceInEquals", "NonFinalFieldReferencedInHashCode"})
@JsonInclude(Include.NON_EMPTY)
public final class TerritoriesBinder extends ApiDataBinder {

    @Nonnull
    private List<TerritoryBinder> territories;

    @Override
    public void validate() {
        validator().start();
        validator().checkNotNull(true, "territories", territories);
        validator().done();
    }

    public TerritoriesBinder(@Nonnull final List<TerritoryBinder> territories) {
        this.territories = territories;
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    private TerritoriesBinder() {
        // Default constructor required by JAX-B.
        super();
    }

    @Nonnull
    public List<TerritoryBinder> getTerritories() {
        beforeGet();
        return territories;
    }

    public void setTerritories(@Nonnull final List<TerritoryBinder> territories) {
        beforeSet();
        assert territories != null;
        this.territories = territories;
    }
}
