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
import com.mapcode.Alphabet;
import com.tomtom.speedtools.apivalidation.ApiDTO;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@SuppressWarnings({"NonFinalFieldReferenceInEquals", "NonFinalFieldReferencedInHashCode", "NullableProblems", "EqualsWhichDoesntCheckParameterClass"})
@JsonInclude(Include.NON_EMPTY)
public final class AlphabetDTO extends ApiDTO {

    @Nonnull
    private String name;

    @Nonnull
    private Integer number;

    @Override
    public void validate() {
        validator().start();
        validator().checkNotNull(true, "name", name);
        validator().checkInteger(true, "numer", number, 0, Alphabet.values().length - 1);
        validator().done();
    }

    public AlphabetDTO(@Nonnull final String name,
                       @Nonnull final Integer number) {
        this.name = name;
        this.number = number;
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    private AlphabetDTO() {
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
    public Integer getNumber() {
        beforeGet();
        return number;
    }

    public void setNumber(@Nonnull final Integer number) {
        beforeSet();
        assert number != null;
        this.number = number;
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
