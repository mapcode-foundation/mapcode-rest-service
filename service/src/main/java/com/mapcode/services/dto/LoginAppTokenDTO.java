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

@XmlRootElement(name = "login")
@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
public final class LoginAppTokenDTO extends ApiDTO {

    @Nullable
    private String appId;
    @Nullable
    private String appToken;

    @Override
    public void validate() {
        validator().start();
        validator().checkUid(true, "appId", appId);
        validator().checkString(true, "appToken", appToken,
                ApiConstants.WEB_APP_TOKEN_MIN_LENGTH,
                ApiConstants.WEB_APP_TOKEN_MAX_LENGTH);
        validator().done();
    }

    public LoginAppTokenDTO(
            @Nonnull final String appId,
            @Nonnull final String appToken) {
        super();
        setAppId(appId);
        setAppToken(appToken);
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    private LoginAppTokenDTO() {
        // Default constructor required by JAX-B.
        super();
    }

    @XmlElement(name = "appId", required = true)
    @Nonnull
    public String getAppId() {
        beforeGet();
        assert appId != null;
        return appId;
    }

    public void setAppId(@Nullable final String appId) {
        beforeSet();
        this.appId = trim(appId);
    }

    @XmlElement(name = "appToken", required = true)
    @Nonnull
    public String getAppToken() {
        beforeGet();
        assert appToken != null;
        return appToken;
    }

    public void setAppToken(@Nullable final String appToken) {
        beforeSet();
        this.appToken = trim(appToken);
    }
}
