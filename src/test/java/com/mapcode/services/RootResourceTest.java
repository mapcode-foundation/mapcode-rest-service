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

import com.tomtom.speedtools.json.Json;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.plugins.server.tjws.TJWSEmbeddedJaxrsServer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;

public class RootResourceTest {
    private static final Logger LOG = LoggerFactory.getLogger(RootResourceTest.class);

    private static final int PORT = 8080;
    private static TJWSEmbeddedJaxrsServer server;

    @BeforeClass
    public static void startServer() {
        server = new TJWSEmbeddedJaxrsServer();
        server.setPort(PORT);
        server.getDeployment().getProviderClasses().add("com.mapcode.services.RootResource");
        server.getDeployment().getProviderClasses().add("com.mapcode.services.MapcodeResource");
        server.start();
    }

    @AfterClass
    public static void stopServer() {
        server.stop();
    }

    @Test
    public void checkVersionJSON() {
        final Response r = new ResteasyClientBuilder().build().target("http://localhost:" + PORT + "/mapcode/version").request().get();
        Assert.assertNotNull(r);
        LOG.info("status = {}", r.getStatus());
        LOG.info("entity = {}", Json.toJson(r.getEntity()));
    }
}
