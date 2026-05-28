/*
 * Copyright (C) 2016-2020, Stichting Mapcode Foundation (http://www.mapcode.com)
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

package com.mapcode.services.standalone;

import com.google.inject.Inject;
import com.mapcode.services.implementation.*;
import com.tomtom.speedtools.maven.MavenProperties;
import com.tomtom.speedtools.rest.Reactor;
import com.tomtom.speedtools.rest.ResourceProcessor;
import com.tomtom.speedtools.testutils.SimpleExecutionContext;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.plugins.interceptors.CacheControlFeature;
import org.jboss.resteasy.plugins.providers.*;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;
import org.jboss.resteasy.plugins.providers.jaxb.*;
import org.jboss.resteasy.plugins.server.tjws.TJWSEmbeddedJaxrsServer;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.ExecutionContext;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.util.List;

public class Server {
    private static final Logger LOG = LoggerFactory.getLogger(Server.class);

    private final MavenProperties mavenProperties;
    private boolean started = false;
    private final TJWSEmbeddedJaxrsServer server;

    @Inject
    public Server(@Nonnull final MavenProperties mavenProperties) {
        this.mavenProperties = mavenProperties;
        server = new TJWSEmbeddedJaxrsServer();
    }

    public synchronized void startServer(final int port) {
        stopServer();
        server.setPort(port);

        /**
         * Create a simple ResourceProcessor, required for implementation of REST service using the
         * SpeedTools framework.
         */
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
        };
        final ResourceProcessor resourceProcessor = new ResourceProcessor(reactor);
        final String bordersFilePath = System.getProperty("mapcode.borders.path",
                System.getenv("MAPCODE_BORDERS_PATH"));
        final BoundaryService boundaryService;
        if (bordersFilePath != null && !bordersFilePath.isEmpty()) {
            boundaryService = new BoundaryService(bordersFilePath);
        } else {
            final InputStream stream = Server.class.getResourceAsStream("/borders.fgb");
            if (stream != null) {
                LOG.info("Server: no borders path configured; loading bundled borders.fgb from classpath");
                boundaryService = new BoundaryService(stream, "classpath:/borders.fgb");
            } else {
                throw new IllegalStateException(
                        "No borders file configured (set mapcode.borders.path or MAPCODE_BORDERS_PATH) " +
                        "and no bundled borders.fgb found on the classpath.");
            }
        }

        LOG.debug("Server: add resources...");
        final ResteasyDeployment deployment = server.getDeployment();
        final List<Object> resources = deployment.getResources();

        // Add mapcode resource.
        final MapcodeResourceImpl mapcodeResource = new MapcodeResourceImpl(
                resourceProcessor,
                boundaryService
        );
        resources.add(mapcodeResource);

        // Add root resource.
        final RootResourceImpl rootResource = new RootResourceImpl(
                mapcodeResource,
                mavenProperties
        );
        resources.add(rootResource);

        // Add JSON and XML mapcode resources.
        resources.add(new OnlyJsonResourceImpl(rootResource, mapcodeResource));
        resources.add(new OnlyXmlResourceImpl(rootResource, mapcodeResource));

        LOG.debug("Server: start server...");
        server.start();

        LOG.debug("Server: register providers...");
        final Dispatcher dispatcher = deployment.getDispatcher();
        final ResteasyProviderFactory providerFactory = dispatcher.getProviderFactory();

        // Slim provider set: JSON (Jackson), XML (JAXB), plain text.
        // The mapcode service has no multipart, form, file, image, or XOP endpoints.
        providerFactory.registerProvider(JAXBXmlSeeAlsoProvider.class, true);
        providerFactory.registerProvider(JAXBXmlRootElementProvider.class, true);
        providerFactory.registerProvider(JAXBElementProvider.class, true);
        providerFactory.registerProvider(JAXBXmlTypeProvider.class, true);
        providerFactory.registerProvider(XmlJAXBContextFinder.class, true);
        providerFactory.registerProvider(DefaultTextPlain.class, true);
        providerFactory.registerProvider(StringTextStar.class, true);
        providerFactory.registerProvider(CacheControlFeature.class, true);
        providerFactory.registerProvider(ResteasyJackson2Provider.class, true);

        LOG.debug("Server: server is ready");
        started = true;
    }

    public synchronized void stopServer() {
        LOG.debug("Server: stop server, started={}", started);
        if (started) {
            server.stop();
            started = false;
        }
    }
}
