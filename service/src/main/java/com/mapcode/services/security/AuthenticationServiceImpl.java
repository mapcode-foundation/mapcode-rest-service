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
package com.mapcode.services.security;


import com.google.inject.Inject;
import com.tomtom.speedtools.checksums.SHA1Hash;
import com.tomtom.speedtools.domain.Uid;
import com.tomtom.speedtools.rest.security.AuthenticationService;
import com.tomtom.speedtools.rest.security.Identity;
import com.tomtom.speedtools.rest.security.Password;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.App;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This class is the implementation of the {@link AuthenticationService}. It contains the logic to
 * authenticate identities.
 */
public class AuthenticationServiceImpl implements AuthenticationService {
    @Nonnull
    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationServiceImpl.class);

    @Inject
    public AuthenticationServiceImpl() {
        LOG.debug("AuthenticationServiceImpl: created");
    }

    @Override
    @Nullable
    public Identity authenticateByUserName(@Nonnull final String userName, @Nonnull final Password password) {
        assert userName != null;
        assert password != null;

        // Hard-coded password check.
        final Identity identity;
        final String hashedPwd = SHA1Hash.hash("123").toString();
        if (hashedPwd.equals(password.getHash())) {
            identity = new Identity(Uid.fromString("1-2-3-4-5"), "mapcode");
        } else {
            identity = null;
        }
        return identity;
    }

    @Override
    @Nullable
    public Identity authenticateByAppId(@Nonnull final Uid<App> appId, @Nonnull final String plaintextToken) {
        assert appId != null;
        assert plaintextToken != null;
        LOG.debug("authenticateByAppId: appId={}", appId.toString());

        // Hard-coded password check.
        final Identity identity;
        if (plaintextToken.equals("123")) {
            identity = new Identity(Uid.fromString("1-2-3-4-5"), "mapcode");
        } else {
            identity = null;
        }
        return identity;
    }
}
