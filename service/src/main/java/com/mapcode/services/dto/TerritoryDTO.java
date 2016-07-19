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
import com.mapcode.services.ApiConstants;
import com.tomtom.speedtools.apivalidation.ApiDTO;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.annotation.*;

@SuppressWarnings({"NonFinalFieldReferenceInEquals", "NonFinalFieldReferencedInHashCode", "NullableProblems", "EqualsWhichDoesntCheckParameterClass"})
@JsonInclude(Include.NON_EMPTY)
@XmlRootElement(name = "territory")
@XmlAccessorType(XmlAccessType.FIELD)
public final class TerritoryDTO extends ApiDTO {

    @XmlElement(name = "alphaCode")
    @Nonnull
    private String alphaCode;

    @XmlElement(name = "alphaCodeMinimalUnambiguous")
    @Nonnull
    private String alphaCodeMinimalUnambiguous;

    @XmlElement(name = "alphaCodeMinimal")
    @Nonnull
    private String alphaCodeMinimal;

    @XmlElement(name = "fullName")
    @Nonnull
    private String fullName;

    @XmlElement(name = "parentTerritory")
    @Nullable
    private String parentTerritory;

    @XmlElementWrapper(name = "aliases")
    @XmlElements(@XmlElement(name = "alias"))
    @Nonnull
    private String[] aliases;

    @XmlElementWrapper(name = "fullNameAliases")
    @XmlElements(@XmlElement(name = "fullNameAlias"))
    @Nonnull
    private String[] fullNameAliases;

    @Override
    public void validate() {
        validator().start();
        validator().checkNotNull(true, "alphaCode", alphaCode);
        validator().checkNotNull(true, "alphaCodeMinimalUnambiguous", alphaCodeMinimalUnambiguous);
        validator().checkNotNull(true, "alphaCodeMinimal", alphaCodeMinimal);
        validator().checkString(true, "fullName", fullName, ApiConstants.API_NAME_LEN_MIN, ApiConstants.API_NAME_LEN_MAX);
        validator().checkString(false, "parentTerritory", parentTerritory, ApiConstants.API_NAME_LEN_MIN, ApiConstants.API_NAME_LEN_MAX);
        validator().checkNotNull(false, "aliases", aliases);
        validator().checkNotNull(false, "fullNameAliases", fullNameAliases);
        validator().done();
    }

    public TerritoryDTO(@Nonnull final String alphaCode,
                        @Nonnull final String alphaCodeMinimalUnambiguous,
                        @Nonnull final String alphaCodeMinimal,
                        @Nonnull final String fullName,
                        @Nullable final String parentTerritory,
                        @Nonnull final String[] aliases,
                        @Nonnull final String[] fullNameAliases) {
        this.alphaCode = alphaCode;
        this.alphaCodeMinimalUnambiguous = alphaCodeMinimalUnambiguous;
        this.alphaCodeMinimal = alphaCodeMinimal;
        this.fullName = fullName;
        this.parentTerritory = parentTerritory;
        this.aliases = aliases;
        this.fullNameAliases = fullNameAliases;
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    private TerritoryDTO() {
        // Default constructor required by JAX-B.
        super();
    }

    @Nonnull
    public String getAlphaCode() {
        beforeGet();
        return alphaCode;
    }

    public void setAlphaCode(@Nonnull final String alphaCode) {
        beforeSet();
        assert alphaCode != null;
        this.alphaCode = alphaCode;
    }

    @Nonnull
    public String getAlphaCodeMinimalUnambiguous() {
        beforeGet();
        return alphaCodeMinimalUnambiguous;
    }

    public void setAlphaCodeMinimalUnambiguous(@Nonnull final String alphaCodeMinimalUnambiguous) {
        beforeSet();
        assert alphaCodeMinimalUnambiguous != null;
        this.alphaCodeMinimalUnambiguous = alphaCodeMinimalUnambiguous;
    }

    @Nonnull
    public String getAlphaCodeMinimal() {
        beforeGet();
        return alphaCodeMinimal;
    }

    public void setAlphaCodeMinimal(@Nonnull final String alphaCodeMinimal) {
        beforeSet();
        assert alphaCodeMinimal != null;
        this.alphaCodeMinimal = alphaCodeMinimal;
    }

    @Nonnull
    public String getFullName() {
        beforeGet();
        return fullName;
    }

    public void setFullName(@Nonnull final String fullName) {
        beforeSet();
        assert fullName != null;
        this.fullName = fullName;
    }

    @Nullable
    public String getParentTerritory() {
        beforeGet();
        return parentTerritory;
    }

    public void setParentTerritory(@Nullable final String parentTerritory) {
        beforeSet();
        assert parentTerritory != null;
        this.parentTerritory = parentTerritory;
    }

    @Nonnull
    public String[] getAliases() {
        beforeGet();
        return aliases;
    }

    public void setAliases(@Nonnull final String[] aliases) {
        beforeSet();
        assert aliases != null;
        this.aliases = aliases;
    }

    @Nonnull
    public String[] getFullNameAliases() {
        beforeGet();
        return fullNameAliases;
    }

    public void setFullNameAliases(@Nonnull final String[] fullNameAliases) {
        beforeSet();
        assert fullNameAliases != null;
        this.fullNameAliases = fullNameAliases;
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
