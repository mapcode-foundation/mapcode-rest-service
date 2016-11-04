/*
 * Copyright (C) 2016, Stichting Mapcode Foundation (http://www.mapcode.com)
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

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@SuppressWarnings({"NonFinalFieldReferenceInEquals", "NonFinalFieldReferencedInHashCode", "NullableProblems", "EqualsWhichDoesntCheckParameterClass"})
@JsonInclude(Include.NON_EMPTY)
@XmlRootElement(name = "alphabet")
@XmlAccessorType(XmlAccessType.FIELD)
public final class AlphabetDTO extends ApiDTO {

    @XmlElement(name = "name")
    @Nonnull
    private String name;

    @Override
    public void validate() {
        validator().start();
        validator().checkNotNull(true, "name", name);
        validator().done();
    }

    public AlphabetDTO(@Nonnull final String name) {
        this.name = name;
    }

    public AlphabetDTO(@Nonnull final Alphabet alphabet) {
        this(alphabet.name());
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
}
