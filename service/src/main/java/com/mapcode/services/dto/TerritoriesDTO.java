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
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.mapcode.Territory;
import com.tomtom.speedtools.apivalidation.ApiDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@SuppressWarnings({"NonFinalFieldReferenceInEquals", "NonFinalFieldReferencedInHashCode", "NullableProblems", "EqualsWhichDoesntCheckParameterClass"})
@ApiModel(
        value = "territories",
        description = "A list of territory objects, such as returned by `GET /mapcode/territories`."
)
@JsonInclude(Include.NON_EMPTY)
@XmlRootElement(name = "territories")
@XmlAccessorType(XmlAccessType.FIELD)
public final class TerritoriesDTO extends ApiDTO {

    @ApiModelProperty(
            name = "total",
            value = "The total number of territory objects (not just the ones in this response)."
    )
    @JsonProperty("total")
    @XmlElement(name = "total")
    @Nonnull
    private int total;

    @ApiModelProperty(
            name = "territories",
            value = "A list of territory objects."
    )
    @JsonProperty("territories")
    @JsonUnwrapped
    @XmlElement(name = "territory")
    @Nonnull
    private TerritoryListDTO territories;

    @Override
    public void validate() {
        validator().start();
        validator().checkInteger(true, "total", total, 0, Territory.values().length);
        validator().checkNotNullAndValidateAll(false, "territories", territories);
        validator().done();
    }

    public TerritoriesDTO(
            final int total,
            @Nonnull final TerritoryListDTO territories) {
        this.total = total;
        this.territories = territories;
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    private TerritoriesDTO() {
        // Default constructor required by JAX-B.
        super();
    }

    public int getTotal() {
        beforeGet();
        return total;
    }

    public void setTotal(final int total) {
        beforeSet();
        this.total = total;
    }

    @Nonnull
    public TerritoryListDTO getTerritories() {
        beforeGet();
        return territories;
    }

    public void setTerritories(@Nonnull final TerritoryListDTO territories) {
        beforeSet();
        this.territories = territories;
    }
}
