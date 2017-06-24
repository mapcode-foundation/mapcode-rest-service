/*
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
package com.mapcode.services.implementation;


import akka.dispatch.Futures;
import com.mapcode.services.SessionResource;
import com.mapcode.services.dto.LoginAppTokenDTO;
import com.mapcode.services.dto.LoginUserNameDTO;
import com.mapcode.services.dto.SessionCreatedDTO;
import com.tomtom.speedtools.apivalidation.exceptions.ApiForbiddenException;
import com.tomtom.speedtools.apivalidation.exceptions.ApiParameterMissingException;
import com.tomtom.speedtools.apivalidation.exceptions.ApiUnauthorizedException;
import com.tomtom.speedtools.domain.Uid;
import com.tomtom.speedtools.rest.ResourceProcessor;
import com.tomtom.speedtools.rest.security.*;
import com.tomtom.speedtools.time.UTCTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.App;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

public class SessionResourceImpl implements SessionResource {
    private static final Logger LOG = LoggerFactory.getLogger(SessionResourceImpl.class);

    @Nonnull
    private final ResourceProcessor processor;
    @Nonnull
    private final AuthenticationService authenticationService;

    @Inject
    public SessionResourceImpl(
            @Nonnull final ResourceProcessor processor,
            @Nonnull final AuthenticationService authenticationService) {
        assert processor != null;
        assert authenticationService != null;

        this.processor = processor;
        this.authenticationService = authenticationService;
    }

    @Override
    public void loginUserName(@Nullable final LoginUserNameDTO loginUserNameDTO,
                              @Nonnull final AsyncResponse response) {
        assert response != null;

        /*
         * Send the handler to an actor for in-Akka execution. The result of the Future is omitted.
         * The purpose of the Future is to schedule the handler only.
         *
         * Note that the Future call() closure has access to the parameter values above.
         */
        final SessionManager sessionManager = new SessionManager();
        processor.process("loginWithLoginDTO", LOG, response, () -> {

            /*
             * Check input parameters.
             */
            if (loginUserNameDTO == null) {
                throw new ApiParameterMissingException("Login details");
            }

            assert loginUserNameDTO != null;
            assert loginUserNameDTO.getUserName() != null;
            assert loginUserNameDTO.getPassword() != null;

            final String userName = loginUserNameDTO.getUserName();
            final String credential = loginUserNameDTO.getPassword();

            // Authenticate.
            final Identity identity = authenticationService.authenticateByUserName(userName,
                    new Password(UTCTime.now(), credential));

            // A identity means authentication was successful.
            if (identity != null) {

                // Create a session
                assert identity != null;
                assert sessionManager != null;

                // Start a session for this Person.
                final String sessionId = sessionManager.startWebSession(identity);
                LOG.debug("loginWithLoginDTO: customer logged in, userName={}", identity.getUserName());

                // Create a response.
                final SessionCreatedDTO binder =
                        new SessionCreatedDTO(sessionId, identity.getId().toString());
                binder.validate();
                response.resume(Response.status(Status.CREATED).entity(binder).build());
            } else {
                throw new ApiUnauthorizedException("Username " + userName);
            }

            // The response is already set within this method body.
            return Futures.successful(null);
        });
    }

    @Override
    public void loginAppToken(
            @Nullable final LoginAppTokenDTO loginAppTokenDTO,
            @Nonnull final AsyncResponse response) {

        assert response != null;

        final SessionManager sessionManager = new SessionManager();
        processor.process("loginAppToken", LOG, response, () -> {

            /*
             * Check input parameters.
             */
            if (loginAppTokenDTO == null) {
                throw new ApiParameterMissingException("App token details");
            }
            assert loginAppTokenDTO != null;

            final Uid<App> appId = Uid.fromString(loginAppTokenDTO.getAppId());
            final String credential = loginAppTokenDTO.getAppToken();

            final Identity identity = authenticationService.authenticateByAppId(appId, credential);
            if (identity != null) {

                // Start an app session for this Person.
                final String sessionId = sessionManager.startAppSession(identity);
                LOG.debug("loginAppToken: customer logged in, userName={}", identity.getUserName());

                // Create a response.
                final SessionCreatedDTO binder =
                        new SessionCreatedDTO(sessionId, identity.getId().toString());
                binder.validate();
                response.resume(Response.status(Status.CREATED).entity(binder).build());
            } else {
                throw new ApiUnauthorizedException("Token " + credential);
            }

            // The response is already set within this method body.
            return Futures.successful(null);
        });
    }

    @Override
    public void logout(
            @Nonnull final String sessionId,
            @Nonnull final AsyncResponse response,
            @Nonnull final SecurityContext securityContext) {

        assert sessionId != null;
        assert response != null;
        assert securityContext != null;
        final String callerTTMainId = SecurityHelper.getPrincipalNameOrThrow(securityContext);

        final SessionManager sessionManager = new SessionManager();

        /*
         * Send the handler to an actor for in-Akka execution. The result of the Future is omitted.
         * The purpose of the Future is to schedule the handler only.
         *
         * Note that the Future call() closure has access to the parameter values above.
         */
        processor.process("logout", LOG, response, () -> {

            @Nullable final String currentSessionId = sessionManager.getCurrentSessionId();
            //noinspection VariableNotUsedInsideIf
            if (currentSessionId == null) {
                // No current session ongoing. This should not occur, because above we already check whether the
                // client is authenticated, which would throw if there was no session.
                LOG.info("logout: client attempted to log out from session \"{}\", but no such session currently " +
                        "exists; continuing as if session would have been successfully terminated.", sessionId);
            }

            // Only the current session can terminate the current session.
            if (sessionId.equals(currentSessionId)) {
                sessionManager.terminateSession();
            } else {
                LOG.info("logout: client attempted to log out from session \"{}\", but client session is \"{}\"; " +
                        "clients are only allowed to terminate their own sessions.", currentSessionId, sessionId);
                throw new ApiForbiddenException("Caller " + callerTTMainId);
            }

            // Report successful logout.
            response.resume(Response.noContent().build());

            // The response is already set within this method body.
            return Futures.successful(null);
        });
    }
}
