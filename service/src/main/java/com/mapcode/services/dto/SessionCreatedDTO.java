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

/**
 * Copyright (C) 2017, TomTom International BV (http://www.tomtom.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mapcode.services.dto;


import com.mapcode.services.ApiConstants;
import com.tomtom.speedtools.apivalidation.ApiDTO;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import static com.tomtom.speedtools.utils.StringUtils.trim;


@XmlRootElement(name = "sessionCreated")
@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
public final class SessionCreatedDTO extends ApiDTO {

    @Nullable
    private String id;
    @Nullable
    private String personId;

    @Override
    public void validate() {
        validator().start();
        validator().checkString(true, "id", id,
                ApiConstants.WEB_ID_MIN_LENGTH,
                ApiConstants.WEB_ID_MAX_LENGTH);
        validator().checkUid(true, "personId", personId);
        validator().done();
    }

    public SessionCreatedDTO(
            @Nonnull final String id,
            @Nonnull final String personId) {
        super();
        setId(id);
        setPersonId(personId);
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    private SessionCreatedDTO() {
        // Default constructor required by JAX-B.
        super();
    }

    @XmlElement(name = "id", required = true)
    @Nonnull
    public String getId() {
        beforeGet();
        assert id != null;
        return id;
    }

    public void setId(@Nullable final String id) {
        beforeSet();
        this.id = trim(id);
    }

    @XmlElement(name = "personId", required = true)
    @Nonnull
    public String getPersonId() {
        beforeGet();
        assert personId != null;
        return personId;
    }

    public void setPersonId(@Nullable final String personId) {
        beforeSet();
        this.personId = trim(personId);
    }
}
