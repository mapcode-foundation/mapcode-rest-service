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
import com.tomtom.speedtools.apivalidation.ApiListDTO;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@SuppressWarnings({"NonFinalFieldReferenceInEquals", "NonFinalFieldReferencedInHashCode", "NullableProblems", "EqualsWhichDoesntCheckParameterClass"})
@JsonInclude(Include.NON_EMPTY)
@XmlRootElement(name = "alphabets")
@XmlAccessorType(XmlAccessType.FIELD)
public final class AlphabetsDTO extends ApiListDTO<AlphabetDTO> {

    @JsonProperty("alphabets")
    @XmlElement(name = "alphabet")
    @Nonnull
    private List<AlphabetDTO> alphabets;

    @Override
    public void validateOne(@Nonnull final AlphabetDTO elm) {
        validator().checkNotNullAndValidate(true, "alphabet", elm);
    }

    public AlphabetsDTO(@Nonnull final List<AlphabetDTO> alphabets) {
        super(alphabets);
        this.alphabets = alphabets;
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    private AlphabetsDTO() {
        // Default constructor required by JAX-B.
        super();
    }

    @Nonnull
    public List<AlphabetDTO> getAlphabets() {
        beforeGet();
        return alphabets;
    }

    public void setAlphabets(@Nonnull final List<AlphabetDTO> alphabets) {
        beforeSet();
        assert alphabets != null;
        this.alphabets = alphabets;
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