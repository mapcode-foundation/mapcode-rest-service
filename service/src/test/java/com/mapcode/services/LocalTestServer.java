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

package com.mapcode.services;

import com.mapcode.services.implementation.*;
import com.tomtom.speedtools.maven.MavenProperties;
import com.tomtom.speedtools.rest.Reactor;
import com.tomtom.speedtools.rest.ResourceProcessor;
import com.tomtom.speedtools.testutils.akka.SimpleExecutionContext;
import org.jboss.resteasy.plugins.server.tjws.TJWSEmbeddedJaxrsServer;
import org.joda.time.DateTime;
import org.junit.After;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.ExecutionContext;

import javax.annotation.Nonnull;

@SuppressWarnings("JUnitTestMethodWithNoAssertions")
public class LocalTestServer {
    private static final Logger LOG = LoggerFactory.getLogger(LocalTestServer.class);

    private static final String HOST = "http://localhost:";
    private static final int PORT = 8081;

    private final TJWSEmbeddedJaxrsServer server;
    private final String version;
    private final int port;

    public LocalTestServer(
            @Nonnull final String version,
            final int port) {
        this.version = version;
        this.port = port;
        server = new TJWSEmbeddedJaxrsServer();
        server.setPort(port);
    }

    public void start() {

        // Create a simple ResourceProcessor, required for implementation of REST service using the SpeedTools framework.
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
        };
        final ResourceProcessor resourceProcessor = new ResourceProcessor(reactor);

        final MavenProperties mavenProperties = new MavenProperties(version);
        final SystemMetricsImpl metrics = new SystemMetricsImpl();

        // Add mapcode resource.
        final MapcodeResourceImpl mapcodeResource = new MapcodeResourceImpl(
                resourceProcessor,
                metrics
        );
        server.getDeployment().getResources().add(mapcodeResource);

        // Add root resource.
        final RootResourceImpl rootResource = new RootResourceImpl(
                mapcodeResource,
                mavenProperties,
                metrics
        );
        server.getDeployment().getResources().add(rootResource);

        // Add JSON and XML mapcode resource.
        server.getDeployment().getResources().add(new OnlyJsonResourceImpl(rootResource, mapcodeResource));
        server.getDeployment().getResources().add(new OnlyXmlResourceImpl(rootResource, mapcodeResource));
        server.start();
        LOG.debug("start: Start local server, baseUrl={}", getBaseUrl());
    }

    @After
    public void stop() {
        LOG.debug("stop: Stop local server, baseUrl={}", getBaseUrl());
        server.stop();
    }

    public int getPort() {
        return port;
    }

    @Nonnull
    public String getBaseUrl() {
        return HOST + PORT;
    }

    @Nonnull
    public String url(@Nonnull final String relativeUrl) {
        return getBaseUrl() + relativeUrl;
    }
}
