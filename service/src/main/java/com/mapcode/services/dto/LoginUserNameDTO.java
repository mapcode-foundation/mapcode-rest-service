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

import static com.tomtom.speedtools.utils.StringUtils.literalToLowerCase;
import static com.tomtom.speedtools.utils.StringUtils.trim;

@XmlRootElement(name = "login")
@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
public final class LoginUserNameDTO extends ApiDTO {

    @Nullable
    private String userName;
    @Nullable
    private String password;

    @Override
    public void validate() {
        validator().start();
        validator().checkEmailAddress(true, "userName", userName);
        validator().checkString(true, "userName", userName,
                ApiConstants.WEB_USER_NAME_MIN_LENGTH,
                ApiConstants.WEB_USER_NAME_MAX_LENGTH);
        validator().checkString(true, "password", password,
                ApiConstants.WEB_PASSWORD_MIN_LENGTH,
                ApiConstants.WEB_PASSWORD_MAX_LENGTH);
        validator().done();
    }

    public LoginUserNameDTO(
            @Nonnull final String userName,
            @Nonnull final String password) {
        super();
        setUserName(userName);
        setPassword(password);
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    private LoginUserNameDTO() {
        // Default constructor required by JAX-B.
        super();
    }

    @XmlElement(name = "userName", required = true)
    @Nonnull
    public String getUserName() {
        beforeGet();
        assert userName != null;
        return userName.toLowerCase();
    }

    public void setUserName(@Nullable final String userName) {
        beforeSet();
        this.userName = literalToLowerCase(trim(userName)); // Treat the same as userName in AccountDTO.
    }

    @XmlElement(name = "password", required = true)
    @Nonnull
    public String getPassword() {
        beforeGet();
        assert password != null;
        return password;
    }

    public void setPassword(@Nullable final String password) {
        beforeSet();
        this.password = password; // Not trimmed, since leading or trailing whitespace might be significant.
    }
}
