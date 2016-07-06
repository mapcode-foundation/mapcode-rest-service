/*
 * Copyright (C) 2016 Stichting Mapcode Foundation (http://www.mapcode.com)
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
 * Copyright (C) 2016 Stichting Mapcode Foundation (http://www.mapcode.com)
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
import com.tomtom.speedtools.objects.Immutables;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.annotation.*;

@SuppressWarnings({"NonFinalFieldReferenceInEquals", "NonFinalFieldReferencedInHashCode", "NullableProblems", "EqualsWhichDoesntCheckParameterClass"})
@JsonInclude(Include.NON_EMPTY)
@XmlRootElement(name = "territories")
@XmlAccessorType(XmlAccessType.FIELD)
public final class TerritoriesDTO extends ApiDTO {

    @JsonProperty("territories")
    @XmlElement(name = "territory")
    @Nonnull
    private TerritoryDTO[] territories;

    @Override
    public void validate() {
        validator().start();
        validator().checkNotNull(true, "territories", territories);
        //noinspection ConstantConditions
        if (territories != null) {
            validator().checkNotNullAndValidateAll(true, "territories", Immutables.listOf(territories));
        }
        validator().done();
    }

    public TerritoriesDTO(@Nonnull final TerritoryDTO[] territories) {
        this.territories = territories;
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    private TerritoriesDTO() {
        // Default constructor required by JAX-B.
        super();
    }

    @Nonnull
    public TerritoryDTO[] getTerritories() {
        beforeGet();
        return territories;
    }

    public void setTerritories(@Nonnull final TerritoryDTO[] territories) {
        beforeSet();
        assert territories != null;
        this.territories = territories;
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
