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
import com.mapcode.services.ApiConstants;
import com.tomtom.speedtools.apivalidation.ApiDTO;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@SuppressWarnings({"NonFinalFieldReferenceInEquals", "NonFinalFieldReferencedInHashCode", "NullableProblems", "EqualsWhichDoesntCheckParameterClass"})
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@XmlRootElement(name = "point")
@XmlAccessorType(XmlAccessType.FIELD)
public final class PointDTO extends ApiDTO {

    @Nonnull
    private Double latDeg;

    @Nonnull
    private Double lonDeg;

    @Override
    public void validate() {
        validator().start();
        validator().checkDouble(true, "latDeg", latDeg, ApiConstants.API_LAT_MIN, ApiConstants.API_LAT_MAX, false);
        validator().checkDouble(true, "lonDeg", lonDeg, -Double.MAX_VALUE, Double.MAX_VALUE, false);
        validator().done();
    }

    public PointDTO(
            @Nonnull final Double latDeg,
            @Nonnull final Double lonDeg) {
        this.latDeg = latDeg;
        this.lonDeg = lonDeg;
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    private PointDTO() {
        // Default constructor required by JAX-B.
        super();
    }

    @Nonnull
    public Double getLatDeg() {
        beforeGet();
        return latDeg;
    }

    public void setLatDeg(@Nonnull final Double latDeg) {
        beforeSet();
        assert latDeg != null;
        this.latDeg = latDeg;
    }

    @Nonnull
    public Double getLonDeg() {
        beforeGet();
        return lonDeg;
    }

    public void setLonDeg(@Nonnull final Double lonDeg) {
        beforeSet();
        assert lonDeg != null;
        this.lonDeg = lonDeg;
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
