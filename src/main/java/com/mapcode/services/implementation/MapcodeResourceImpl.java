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

package com.mapcode.services.implementation;

import akka.dispatch.Futures;
import com.tomtom.speedtools.apivalidation.exceptions.ApiException;
import com.tomtom.speedtools.apivalidation.exceptions.ApiForbiddenException;
import com.mapcode.services.MapcodeResource;
import com.mapcode.services.domain.MapcodeResultBinder;
import com.tomtom.speedtools.rest.ResourceProcessor;
import org.jboss.resteasy.spi.AsynchronousResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

/**
 * This class implements the REST API that deals with TTBin files.
 */
public class MapcodeResourceImpl implements MapcodeResource {
    private static final Logger LOG = LoggerFactory.getLogger(MapcodeResourceImpl.class);

    private static final String VALID_API_KEY = "0";

    @Nonnull
    private final ResourceProcessor processor;

    @Inject
    public MapcodeResourceImpl(@Nonnull final ResourceProcessor processor) {
        assert processor != null;
        this.processor = processor;
    }

    @Override
    public void getMapcodeFrom(
            @Nonnull final String apiKey,
            @Nonnull final String latDeg,
            @Nonnull final String lonDeg,
            @Nonnull final String precision,
            @Nonnull final AsynchronousResponse response) throws ApiException {
        assert apiKey != null;
        assert latDeg != null;
        assert lonDeg != null;
        assert precision != null;
        assert response != null;

        processor.process("getMapcodeFrom", LOG, response, () -> {
            LOG.debug("getMapcodeFrom: apiKey={}, latDeg={}, lonDeg={}, precision={}", apiKey, latDeg, lonDeg, precision);

            if (!apiKey.equals(VALID_API_KEY)) {
                throw new ApiForbiddenException("apiKey must be valid");
            }

            final MapcodeResultBinder binder = new MapcodeResultBinder("49.4V");
            binder.validate();
            response.setResponse(Response.ok(binder).build());

            // The response is already set within this method body.
            return Futures.successful(null);
        });
    }
}
