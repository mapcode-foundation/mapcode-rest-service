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
import com.mapcode.Territory;
import com.mapcode.Territory.AlphaCodeFormat;
import com.tomtom.speedtools.apivalidation.ApiListDTO;

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings({"NonFinalFieldReferenceInEquals", "NonFinalFieldReferencedInHashCode", "NullableProblems", "EqualsWhichDoesntCheckParameterClass"})
@JsonInclude(Include.NON_EMPTY)
@XmlRootElement(name = "territories")
@XmlAccessorType(XmlAccessType.FIELD)
public final class TerritoryListDTO extends ApiListDTO<TerritoryDTO> {

    @Override
    public void validateOne(@Nonnull final TerritoryDTO elm) {
        validator().checkNotNullAndValidate(true, "territory", elm);
    }

    public TerritoryListDTO(@Nonnull final List<TerritoryDTO> territories) {
        super(territories);
    }

    public TerritoryListDTO(@Nonnull final Territory[] territories) {
        this(Arrays.asList(territories).stream().map(x -> {
            return new TerritoryDTO(
                    x.toString(),
                    x.toAlphaCode(AlphaCodeFormat.MINIMAL_UNAMBIGUOUS),
                    x.toAlphaCode(AlphaCodeFormat.MINIMAL),
                    x.getFullName(),
                    (x.getParentTerritory() == null) ? null : x.getParentTerritory().toString(),
                    x.getAliases(),
                    x.getFullNameAliases(),
                    x.getAlphabets()
            );
        }).collect(Collectors.toList()));
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    private TerritoryListDTO() {
        // Default constructor required by JAX-B.
        super();
    }
}
