/*
 * Copyright (C) 2016-2019, Stichting Mapcode Foundation (http://www.mapcode.com)
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

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@SuppressWarnings("JUnitTestMethodWithNoAssertions")
public class ApiCoordsTest {
    private static final Logger LOG = LoggerFactory.getLogger(ApiCoordsTest.class);

    private static final String TEST_CODE1 = "VJ0L6.9PNQ";
    private static final String TEST_CODE2 = "JL0.KP";
    private static final String TEST_CONTEXT2 = "LUX";

    private LocalTestServer server;

    @Before
    public void startServer() {
        server = new LocalTestServer("1.0", 8081);
        server.start();
    }

    @After
    public void stopServer() {
        server.stop();
    }

    @Test
    public void checkCoords1Json() {
        LOG.info("checkCoords1Json");
        final Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/coords/" + TEST_CODE1)).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"latDeg\":50.141726,\"lonDeg\":6.1358875}",
                response.readEntity(String.class));
    }

    @Test
    public void checkCoords1Xml() {
        LOG.info("checkCoords1Xml");
        final Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/coords/" + TEST_CODE1)).
                request().
                accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><point><latDeg>50.141726</latDeg><lonDeg>6.1358875</lonDeg></point>",
                response.readEntity(String.class));
    }

    @Test
    public void checkCoords2Json() {
        LOG.info("checkCoords2Json");
        final Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/coords/" + TEST_CODE2 + "?context=" + TEST_CONTEXT2)).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"latDeg\":50.141735,\"lonDeg\":6.135845}",
                response.readEntity(String.class));
    }

    @Test
    public void checkCoords2Xml() {
        LOG.info("checkCoords2Xml");
        final Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/coords/" + TEST_CODE2 + "?context=" + TEST_CONTEXT2)).
                request().
                accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><point><latDeg>50.141735</latDeg><lonDeg>6.135845</lonDeg></point>",
                response.readEntity(String.class));
    }
}
