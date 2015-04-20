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

package com.mapcode.services;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.mapcode.services.implementation.MapcodeResourceImpl;
import com.mapcode.services.implementation.RootResourceImpl;
import com.tomtom.speedtools.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Singleton;

/**
 * This module binds service implementations to their interfaces.
 */
public class ServicesModule implements Module {
    private static final Logger LOG = LoggerFactory.getLogger(ServicesModule.class);

    @Override
    public void configure(@Nonnull final Binder binder) {
        assert binder != null;

        // Bind APIs to their implementation.
        binder.bind(RootResource.class).to(RootResourceImpl.class).in(Singleton.class);
        binder.bind(MapcodeResource.class).to(MapcodeResourceImpl.class).in(Singleton.class);

        LOG.info("configure:");
        LOG.info("configure: GET /help -- Get help text for web services");

        // Add some additional features for string (human readable) mappers.
        Json.getCurrentStringObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, false);
    }
}
