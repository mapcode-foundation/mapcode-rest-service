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

import com.google.inject.Inject;
import com.mapcode.services.implementation.MapcodeResourceImpl;
import com.mapcode.services.implementation.RootResourceImpl;
import com.mapcode.services.implementation.SystemMetricsImpl;
import com.tomtom.speedtools.maven.MavenProperties;
import com.tomtom.speedtools.rest.Reactor;
import com.tomtom.speedtools.rest.ResourceProcessor;
import com.tomtom.speedtools.testutils.akka.SimpleExecutionContext;
import org.jboss.resteasy.plugins.server.tjws.TJWSEmbeddedJaxrsServer;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.ExecutionContext;

import javax.annotation.Nonnull;

public class Server {
    private static final Logger LOG = LoggerFactory.getLogger(Server.class);

    private static final int PORT = 8080;
    private static final String HOST = "http://localhost:" + PORT;

    private final TJWSEmbeddedJaxrsServer server;

    @Inject
    public Server(@Nonnull final MavenProperties mavenProperties) {
        server = new TJWSEmbeddedJaxrsServer();
        server.setPort(PORT);

        // Create a simple ResourceProcessor, required for implementation of REST service using the SpeedTools framework.
        LOG.debug("Server: create execution context...");
        final Reactor reactor = new Reactor() {
            @Nonnull
            @Override
            public ExecutionContext getExecutionContext() {
                return SimpleExecutionContext.getInstance();
            }

            // This method is stubbed and never used.
            @Nonnull
            @Override
            public DateTime getSystemStartupTime() {
                return new DateTime();
            }

            // This method is stubbed and never used.
            @Nonnull
            @Override
            public <T> T createTopLevelActor(@Nonnull final Class<T> interfaceClass, @Nonnull final Class<? extends T> implementationClass, @Nonnull Object... explicitParameters) {
                assert false;
                return null;
            }
        };
        final ResourceProcessor resourceProcessor = new ResourceProcessor(reactor);
        final SystemMetricsImpl metrics = new SystemMetricsImpl();
        LOG.debug("Server: add resources...");

        // Add root resource.
        server.getDeployment().getResources().add(new RootResourceImpl(
                mavenProperties,
                metrics
        ));

        // Add mapcode resource.
        server.getDeployment().getResources().add(new MapcodeResourceImpl(
                resourceProcessor,
                metrics
        ));
    }

    public void startServer() {
        LOG.debug("Server: start server...");
        server.start();
    }

    public void stopServer() {
        LOG.debug("Server: stop server...");
        server.stop();
    }
}
