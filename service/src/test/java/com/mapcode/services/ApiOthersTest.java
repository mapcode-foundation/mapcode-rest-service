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

package com.mapcode.services;

import com.google.gson.Gson;
import com.mapcode.services.dto.VersionDTO;
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
public class ApiOthersTest {
    private static final Logger LOG = LoggerFactory.getLogger(ApiOthersTest.class);

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
    public void checkStatusXmlJson() {
        LOG.info("checkStatusXmlJson");
        Response request = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/status")).
                request().
                get();
        Assert.assertNotNull(request);
        int status = request.getStatus();
        LOG.info("status = {}", status);
        Assert.assertEquals(200, status);

        request = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/xml/status")).
                request().
                get();
        Assert.assertNotNull(request);
        status = request.getStatus();
        LOG.info("status = {}", status);
        Assert.assertEquals(200, status);

        request = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/json/status")).
                request().
                get();
        Assert.assertNotNull(request);
        status = request.getStatus();
        LOG.info("status = {}", status);
        Assert.assertEquals(200, status);
    }

    @Test
    public void getHelp() {
        LOG.info("getHelp");
        final Response r = new ResteasyClientBuilder().build().
                target(server.url("/mapcode")).
                request().
                get();
        Assert.assertNotNull(r);
        final int status = r.getStatus();
        LOG.info("status = {}", status);
        Assert.assertEquals(200, status);
        Assert.assertEquals("<html>", r.readEntity(String.class).substring(0, 6));
    }

    @Test
    public void checkVersionJson() {
        LOG.info("checkVersionJson");
        final Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/version")).
                request().
                accept(MediaType.APPLICATION_JSON).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        final String s = response.readEntity(String.class);
        Assert.assertEquals("{\"version\":\"1.0\"}",
                s);
        final VersionDTO x = new Gson().fromJson(s, VersionDTO.class);
        Assert.assertNotNull(x);
        Assert.assertEquals("1.0", x.getVersion());
    }

    @Test
    public void checkVersionXmlJson() {
        LOG.info("checkVersionXmlJson");
        final String expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><version><version>1.0</version></version>";
        final String expectedJson = "{\"version\":\"1.0\"}";
        Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/version")).
                request().
                accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(expectedXml, response.readEntity(String.class));

        response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/xml/version")).
                request().
                get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(expectedXml, response.readEntity(String.class));

        response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/json/version")).
                request().
                get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(expectedJson, response.readEntity(String.class));
    }

    @Test
    public void checkMetrics() {
        LOG.info("checkMetrics");
        final Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/metrics")).
                request().
                accept(MediaType.APPLICATION_JSON).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        final String s = response.readEntity(String.class);
        Assert.assertEquals("{\"allMapcodeToLatLonRequests\":{\"calculators\":[{\"totalMetricDuration\":{\"iMillis\":2592000000},\"timeSlotDuration\":{\"iMillis\":86400000},\"values\":[],\"sum\":0.0,\"count\":0,\"sumSquares\":0.0},{\"totalMetricDuration\":{\"iMillis\":604800000},\"timeSlotDuration\":{\"iMillis\":3600000},\"values\":[],\"sum\":0.0,\"count\":0,\"sumSquares\":0.0},{\"totalMetricDuration\":{\"iMillis\":86400000},\"timeSlotDuration\":{\"iMillis\":1800000},\"values\":[],\"sum\":0.0,\"count\":0,\"sumSquares\":0.0},{\"totalMetricDuration\":{\"iMillis\":3600000},\"timeSlo",
                s.substring(0, 500));
        Assert.assertEquals("illis\":604800000},\"timeSlotDuration\":{\"iMillis\":3600000},\"values\":[],\"sum\":0.0,\"count\":0,\"sumSquares\":0.0},{\"totalMetricDuration\":{\"iMillis\":86400000},\"timeSlotDuration\":{\"iMillis\":1800000},\"values\":[],\"sum\":0.0,\"count\":0,\"sumSquares\":0.0},{\"totalMetricDuration\":{\"iMillis\":3600000},\"timeSlotDuration\":{\"iMillis\":60000},\"values\":[],\"sum\":0.0,\"count\":0,\"sumSquares\":0.0},{\"totalMetricDuration\":{\"iMillis\":60000},\"timeSlotDuration\":{\"iMillis\":2000},\"values\":[],\"sum\":0.0,\"count\":0,\"sumSquares\":0.0}]}}}",
                s.substring(s.length() - 500, s.length()));
    }
}
