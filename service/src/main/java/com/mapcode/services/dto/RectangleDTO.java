/*
 * Copyright (C) 2016-2019, Stichting Mapcode Foundation (http://www.mapcode.com)
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
import com.mapcode.Rectangle;
import com.tomtom.speedtools.apivalidation.ApiDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@SuppressWarnings({"NullableProblems", "InstanceVariableMayNotBeInitialized"})
@ApiModel(
        value = "rectangle",
        description = "A rectangular geospatial area, defined by its South-West and North-East corners.")
@JsonInclude(Include.NON_EMPTY)
@XmlRootElement(name = "rectangle")
@XmlAccessorType(XmlAccessType.FIELD)
public final class RectangleDTO extends ApiDTO {

    @ApiModelProperty(
            name = "southWest",
            value = "The South-West corner of the rectangular area.")
    @XmlElement(name = "southWest")
    @Nonnull
    private PointDTO southWest;

    @ApiModelProperty(
            name = "northEast",
            value = "The North-East corner of the rectangular area.")
    @XmlElement(name = "northEast")
    @Nonnull
    private PointDTO northEast;

    @ApiModelProperty(
            name = "center",
            value = "The center coordinate of the rectangular area.")
    @XmlElement(name = "center")
    @Nullable
    private PointDTO center;

    @Override
    public void validate() {
        validator().start();
        validator().checkNotNullAndValidate(true, "southWest", southWest);
        validator().checkNotNullAndValidate(true, "northEast", northEast);
        validator().checkNotNullAndValidate(false, "center", center);
        validator().done();
    }

    public RectangleDTO(
            @Nonnull final PointDTO southWest,
            @Nonnull final PointDTO northEast,
            @Nullable final PointDTO center) {
        this.southWest = southWest;
        this.northEast = northEast;
        this.center = center;
    }

    public RectangleDTO(
            @Nonnull final PointDTO southWest,
            @Nonnull final PointDTO northEast) {
        this(southWest, northEast,
                new PointDTO((southWest.getLatDeg() + northEast.getLatDeg()) / 2.0,
                        (southWest.getLonDeg() + northEast.getLonDeg()) / 2.0));
    }

    public RectangleDTO(@Nonnull final Rectangle rectangle) {
        this(new PointDTO(rectangle.getSouthWest()), new PointDTO(rectangle.getNorthEast()));
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    private RectangleDTO() {
        // Default constructor required by JAX-B.
        super();
    }

    @Nonnull
    public PointDTO getSouthWest() {
        beforeGet();
        return southWest;
    }

    public void setSouthWest(@Nonnull final PointDTO southWest) {
        beforeSet();
        assert southWest != null;
        this.southWest = southWest;
    }

    @Nonnull
    public PointDTO getNorthEast() {
        beforeGet();
        return northEast;
    }

    public void setNorthEast(@Nonnull final PointDTO northEast) {
        beforeSet();
        assert northEast != null;
        this.northEast = northEast;
    }

    @Nullable
    public PointDTO getCenter() {
        beforeGet();
        return center;
    }

    public void setCenter(@Nullable final PointDTO center) {
        beforeSet();
        this.center = center;
    }
}
