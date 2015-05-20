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

package com.mapcode.services.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.mapcode.Territory;
import com.mapcode.services.ApiConstants;
import com.tomtom.speedtools.apivalidation.ApiDTO;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@SuppressWarnings({"NonFinalFieldReferenceInEquals", "NonFinalFieldReferencedInHashCode", "NullableProblems", "EqualsWhichDoesntCheckParameterClass"})
@JsonInclude(Include.NON_EMPTY)
public final class TerritoryDTO extends ApiDTO {

    @Nonnull
    private String name;

    @Nonnull
    private String nameMinimalUnambiguous;

    @Nonnull
    private String nameMinimal;

    @Nonnull
    private Integer code;

    @Nonnull
    private String fullName;

    @Nullable
    private String parentTerritory;

    @Nonnull
    private String[] aliases;

    @Nonnull
    private String[] fullNameAliases;

    @Override
    public void validate() {
        validator().start();
        validator().checkNotNull(true, "name", name);
        validator().checkNotNull(true, "nameMinimalUnambiguous", nameMinimalUnambiguous);
        validator().checkNotNull(true, "nameMinimal", nameMinimal);
        validator().checkInteger(true, "code", code, 0, Territory.values().length - 1);
        validator().checkString(true, "fullName", fullName, ApiConstants.API_NAME_LEN_MIN, ApiConstants.API_NAME_LEN_MAX);
        validator().checkString(false, "parentTerritory", parentTerritory, ApiConstants.API_NAME_LEN_MIN, ApiConstants.API_NAME_LEN_MAX);
        validator().checkNotNull(false, "aliases", aliases);
        validator().checkNotNull(false, "fullNameAliases", fullNameAliases);
        validator().done();
    }

    public TerritoryDTO(@Nonnull final String name,
                        @Nonnull final String nameMinimalUnambiguous,
                        @Nonnull final String nameMinimal,
                        @Nonnull final Integer code,
                        @Nonnull final String fullName,
                        @Nullable final String parentTerritory,
                        @Nonnull final String[] aliases,
                        @Nonnull final String[] fullNameAliases) {
        this.name = name;
        this.nameMinimalUnambiguous = nameMinimalUnambiguous;
        this.nameMinimal = nameMinimal;
        this.code = code;
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
    public String getName() {
        beforeGet();
        return name;
    }

    public void setName(@Nonnull final String name) {
        beforeSet();
        assert name != null;
        this.name = name;
    }

    @Nonnull
    public String getNameMinimalUnambiguous() {
        beforeGet();
        return nameMinimalUnambiguous;
    }

    public void setNameMinimalUnambiguous(@Nonnull final String nameMinimalUnambiguous) {
        beforeSet();
        assert nameMinimalUnambiguous != null;
        this.nameMinimalUnambiguous = nameMinimalUnambiguous;
    }

    @Nonnull
    public String getNameMinimal() {
        beforeGet();
        return nameMinimal;
    }

    public void setNameMinimal(@Nonnull final String nameMinimal) {
        beforeSet();
        assert nameMinimal != null;
        this.nameMinimal = nameMinimal;
    }

    @Nonnull
    public Integer getCode() {
        beforeGet();
        return code;
    }

    public void setCode(@Nonnull final Integer code) {
        beforeSet();
        assert code != null;
        this.code = code;
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
