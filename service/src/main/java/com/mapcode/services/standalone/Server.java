/*
 * Copyright (C) 2016, Stichting Mapcode Foundation (http://www.mapcode.com)
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
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.plugins.interceptors.CacheControlFeature;
import org.jboss.resteasy.plugins.interceptors.encoding.AcceptEncodingGZIPFilter;
import org.jboss.resteasy.plugins.interceptors.encoding.AcceptEncodingGZIPInterceptor;
import org.jboss.resteasy.plugins.interceptors.encoding.ClientContentEncodingAnnotationFeature;
import org.jboss.resteasy.plugins.interceptors.encoding.GZIPDecodingInterceptor;
import org.jboss.resteasy.plugins.interceptors.encoding.GZIPEncodingInterceptor;
import org.jboss.resteasy.plugins.interceptors.encoding.ServerContentEncodingAnnotationFeature;
import org.jboss.resteasy.plugins.providers.*;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;
import org.jboss.resteasy.plugins.providers.jaxb.CollectionProvider;
import org.jboss.resteasy.plugins.providers.jaxb.JAXBElementProvider;
import org.jboss.resteasy.plugins.providers.jaxb.JAXBXmlRootElementProvider;
import org.jboss.resteasy.plugins.providers.jaxb.JAXBXmlSeeAlsoProvider;
import org.jboss.resteasy.plugins.providers.jaxb.JAXBXmlTypeProvider;
import org.jboss.resteasy.plugins.providers.jaxb.MapProvider;
import org.jboss.resteasy.plugins.providers.jaxb.XmlJAXBContextFinder;
import org.jboss.resteasy.plugins.providers.multipart.*;
import org.jboss.resteasy.plugins.server.tjws.TJWSEmbeddedJaxrsServer;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.ExecutionContext;

import javax.annotation.Nonnull;
import java.util.List;

public class Server {
    private static final Logger LOG = LoggerFactory.getLogger(Server.class);

    private final MavenProperties mavenProperties;
    private final TJWSEmbeddedJaxrsServer server;

    @Inject
    public Server(@Nonnull final MavenProperties mavenProperties) {
        this.mavenProperties = mavenProperties;
        server = new TJWSEmbeddedJaxrsServer();
    }

    public void startServer(final int port) {
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
        final ResteasyDeployment deployment = server.getDeployment();
        final List<Object> resources = deployment.getResources();

        // Add root resource.
        resources.add(new RootResourceImpl(
                mavenProperties,
                metrics
        ));

        // Add mapcode resource.
        resources.add(new MapcodeResourceImpl(
                resourceProcessor,
                metrics
        ));

        LOG.debug("Server: start server...");
        server.start();

        LOG.debug("Server: register providers...");
        final Dispatcher dispatcher = deployment.getDispatcher();
        final ResteasyProviderFactory providerFactory = dispatcher.getProviderFactory();

        /**
         * Register providers for JSON and XML. This list was obtained by looking at which providers
         * were registered when the application would be running its unit tests, which works as well.
         */
        providerFactory.registerProvider(JAXBXmlSeeAlsoProvider.class, true);
        providerFactory.registerProvider(JAXBXmlRootElementProvider.class, true);
        providerFactory.registerProvider(JAXBElementProvider.class, true);
        providerFactory.registerProvider(JAXBXmlTypeProvider.class, true);
        providerFactory.registerProvider(CollectionProvider.class, true);
        providerFactory.registerProvider(MapProvider.class, true);
        providerFactory.registerProvider(XmlJAXBContextFinder.class, true);
        providerFactory.registerProvider(DataSourceProvider.class, true);
        providerFactory.registerProvider(DocumentProvider.class, true);
        providerFactory.registerProvider(DefaultTextPlain.class, true);
        providerFactory.registerProvider(StringTextStar.class, true);
        providerFactory.registerProvider(SourceProvider.class, true);
        providerFactory.registerProvider(InputStreamProvider.class, true);
        providerFactory.registerProvider(ReaderProvider.class, true);
        providerFactory.registerProvider(ByteArrayProvider.class, true);
        providerFactory.registerProvider(FormUrlEncodedProvider.class, true);
        providerFactory.registerProvider(JaxrsFormProvider.class, true);
        providerFactory.registerProvider(FileProvider.class, true);
        providerFactory.registerProvider(FileRangeWriter.class, true);
        providerFactory.registerProvider(StreamingOutputProvider.class, true);
        providerFactory.registerProvider(IIOImageProvider.class, true);
        providerFactory.registerProvider(SerializableProvider.class, true);
        providerFactory.registerProvider(CacheControlFeature.class, true);
        providerFactory.registerProvider(AcceptEncodingGZIPInterceptor.class, true);
        providerFactory.registerProvider(AcceptEncodingGZIPFilter.class, true);
        providerFactory.registerProvider(ClientContentEncodingAnnotationFeature.class, true);
        providerFactory.registerProvider(GZIPDecodingInterceptor.class, true);
        providerFactory.registerProvider(GZIPEncodingInterceptor.class, true);
        providerFactory.registerProvider(ServerContentEncodingAnnotationFeature.class, true);
        providerFactory.registerProvider(ResteasyJackson2Provider.class, true);
        providerFactory.registerProvider(MultipartReader.class, true);
        providerFactory.registerProvider(ListMultipartReader.class, true);
        providerFactory.registerProvider(MultipartFormDataReader.class, true);
        providerFactory.registerProvider(MultipartRelatedReader.class, true);
        providerFactory.registerProvider(MapMultipartFormDataReader.class, true);
        providerFactory.registerProvider(MultipartWriter.class, true);
        providerFactory.registerProvider(MultipartFormDataWriter.class, true);
        providerFactory.registerProvider(MultipartRelatedWriter.class, true);
        providerFactory.registerProvider(ListMultipartWriter.class, true);
        providerFactory.registerProvider(MapMultipartFormDataWriter.class, true);
        providerFactory.registerProvider(MultipartFormAnnotationReader.class, true);
        providerFactory.registerProvider(MultipartFormAnnotationWriter.class, true);
        providerFactory.registerProvider(MimeMultipartProvider.class, true);
        providerFactory.registerProvider(XopWithMultipartRelatedReader.class, true);
        providerFactory.registerProvider(XopWithMultipartRelatedWriter.class, true);

        LOG.debug("Server: server is ready");
    }

    public void stopServer() {
        LOG.debug("Server: stop server...");
        server.stop();
    }
}
