/*
 * Copyright (C) 2016-2018, Stichting Mapcode Foundation (http://www.mapcode.com)
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
import com.mapcode.Alphabet;
import com.mapcode.services.ApiConstants;
import com.tomtom.speedtools.apivalidation.ApiDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.annotation.*;

@SuppressWarnings({"NonFinalFieldReferenceInEquals", "NonFinalFieldReferencedInHashCode", "NullableProblems", "EqualsWhichDoesntCheckParameterClass"})
@ApiModel(
        value = "territory",
        description = "A territory definition object, such as returned by `GET /mapcode/territories/nld`."
)
@JsonInclude(Include.NON_EMPTY)
@XmlRootElement(name = "territory")
@XmlAccessorType(XmlAccessType.FIELD)
public final class TerritoryDTO extends ApiDTO {

    @ApiModelProperty(
            name = "alphaCode",
            value = "The alpha-code of the territory. This is a 3-character `XXX` code, or a 2-2 character `XX-YY` code, " +
                    "for territories which are divided into sub-regions.")
    @XmlElement(name = "alphaCode")
    @Nonnull
    private String alphaCode;

    @ApiModelProperty(
            name = "alphaCodeMinimalUnambiguous",
            value = "The shortest alpha-code which is still unambiguous, world-wide. For example, `TX` is unambiguous, " +
                    "world-wide, for Texas in the USA, but `IN` is not: it can be India or Indiana, USA.")
    @XmlElement(name = "alphaCodeMinimalUnambiguous")
    @Nonnull
    private String alphaCodeMinimalUnambiguous;

    @ApiModelProperty(
            name = "alphaCodeMinimal",
            value = "The shortest alpha-code. This code may be ambiguous for `XX-YY` types of codes, if used without its " +
                    "parent territory.")
    @XmlElement(name = "alphaCodeMinimal")
    @Nonnull
    private String alphaCodeMinimal;

    @ApiModelProperty(
            name = "fullName",
            value = "The full name (in English) of the territory.")
    @XmlElement(name = "fullName")
    @Nonnull
    private String fullName;

    @ApiModelProperty(
            name = "parentTerritory",
            value = "(optional) The full name (in English) of the parent territory. This is only relevant for `XX-YY` codes.")
    @XmlElement(name = "parentTerritory")
    @Nullable
    private String parentTerritory;

    @ApiModelProperty(
            name = "aliases",
            value = "Alias territory codes for the territory. For example, `US-VI` and `USA-VI` both designate " +
                    "the Virgin Islands.")
    @XmlElementWrapper(name = "aliases")
    @XmlElements(@XmlElement(name = "alias"))
    @Nonnull
    private String[] aliases;

    @ApiModelProperty(
            name = "fullNameAliases",
            value = "Alias names (in English) of the territory. For example, `GBR` designates the United Kingdom, " +
                    "but it has aliases, such as Great Britain, Scotland and more.")
    @XmlElementWrapper(name = "fullNameAliases")
    @XmlElements(@XmlElement(name = "fullNameAlias"))
    @Nonnull
    private String[] fullNameAliases;

    @ApiModelProperty(
            name = "alphabets",
            value = "A list of typical alphabets (scripts) used in this territory, ordered by importance. " +
                    "`ROMAN` is always present in this list, as a fallback.",
            dataType = "com.mapcode.services.dto.AlphabetsDTO",
            reference = "com.mapcode.services.dto.AlphabetsDTO")
    @JsonProperty("alphabets")
    @JsonUnwrapped
    @XmlElementWrapper(name = "alphabets")
    @XmlElement(name = "alphabet")
    @Nonnull
    private AlphabetListDTO alphabets;

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
        validator().checkNotNullAndValidateAll(true, "alphabets", alphabets);
        validator().done();
    }

    public TerritoryDTO(@Nonnull final String alphaCode,
                        @Nonnull final String alphaCodeMinimalUnambiguous,
                        @Nonnull final String alphaCodeMinimal,
                        @Nonnull final String fullName,
                        @Nullable final String parentTerritory,
                        @Nonnull final String[] aliases,
                        @Nonnull final String[] fullNameAliases,
                        @Nonnull final AlphabetListDTO alphabets) {
        this.alphaCode = alphaCode;
        this.alphaCodeMinimalUnambiguous = alphaCodeMinimalUnambiguous;
        this.alphaCodeMinimal = alphaCodeMinimal;
        this.fullName = fullName;
        this.parentTerritory = parentTerritory;
        this.aliases = aliases;
        this.fullNameAliases = fullNameAliases;
        this.alphabets = alphabets;
    }

    public TerritoryDTO(@Nonnull final String alphaCode,
                        @Nonnull final String alphaCodeMinimalUnambiguous,
                        @Nonnull final String alphaCodeMinimal,
                        @Nonnull final String fullName,
                        @Nullable final String parentTerritory,
                        @Nonnull final String[] aliases,
                        @Nonnull final String[] fullNameAliases,
                        @Nonnull final Alphabet[] alphabets) {
        this(alphaCode, alphaCodeMinimalUnambiguous, alphaCodeMinimal, fullName, parentTerritory, aliases, fullNameAliases,
                new AlphabetListDTO(alphabets));
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

    @Nonnull
    public AlphabetListDTO getAlphabets() {
        beforeGet();
        return alphabets;
    }

    public void setAlphabets(@Nonnull final AlphabetListDTO alphabets) {
        beforeSet();
        this.alphabets = alphabets;
    }
}
