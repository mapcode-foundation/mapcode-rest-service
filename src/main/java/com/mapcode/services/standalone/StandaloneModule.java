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

package com.mapcode.services.standalone;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.mapcode.services.deployment.StartupCheck;
import com.tomtom.speedtools.maven.MavenProperties;
import com.tomtom.speedtools.tracer.LoggingTraceHandler;
import com.tomtom.speedtools.tracer.TracerFactory;
import com.tomtom.speedtools.tracer.mongo.MongoDBTraceHandler;
import com.tomtom.speedtools.tracer.mongo.MongoDBTraceProperties;
import com.tomtom.speedtools.tracer.mongo.MongoDBTraceStream;

import javax.annotation.Nonnull;

/**
 * Google Guice Module that contains the binding for the CheckDB class.
 */
public class StandaloneModule implements Module {

    @Override
    public void configure(@Nonnull final Binder binder) {
        assert binder != null;

        TracerFactory.setEnabled(true);
        binder.bind(MongoDBTraceProperties.class).asEagerSingleton();
        binder.bind(MongoDBTraceStream.class);
        binder.bind(MongoDBTraceHandler.class).asEagerSingleton();
        binder.bind(LoggingTraceHandler.class).asEagerSingleton();
        binder.bind(MavenProperties.class).in(Singleton.class);
        binder.bind(StartupCheck.class).asEagerSingleton();
        binder.bind(Server.class).asEagerSingleton();
    }
}
