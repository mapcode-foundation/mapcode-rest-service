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

package com.mapcode.services.standalone;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@SuppressWarnings({"rawtypes", "ProhibitedExceptionDeclared", "unchecked"})
public class MainCommandLineTest {
    private static final Logger LOG = LoggerFactory.getLogger(MainCommandLineTest.class);

    private static final int SERVER_PORT = 8081;

    @SuppressWarnings("JUnitTestMethodWithNoAssertions")
    @Test
    public void testHelp() throws Exception {
        LOG.info("testHelp");

        // Initialize Mockito.
        MockitoAnnotations.initMocks(this);
        MainCommandLine.execute(new String[]{"--help"});
    }

    @SuppressWarnings("JUnitTestMethodWithNoAssertions")
    @Test
    public void testUnknownArgument() throws Exception {
        LOG.info("testUnknownArgument");

        // Initialize Mockito.
        MockitoAnnotations.initMocks(this);
        MainCommandLine.execute(new String[]{"--help", "unknown"});
        MainCommandLine.execute(new String[]{"--unknown"});
        MainCommandLine.execute(new String[]{"--port"});
    }

    @SuppressWarnings("JUnitTestMethodWithNoAssertions")
    @Test
    public void testServer() throws Exception {
        LOG.info("testServer");

        // Initialize Mockito.
        MockitoAnnotations.initMocks(this);
        MainCommandLine.execute(new String[]{"--silent", "--debug", "--port", "8081"});

        // Execute a REST API call.
        checkVersionXmlJson();

        MainCommandLine.stop();
    }

    public void checkVersionXmlJson() {
        LOG.info("checkVersionXmlJson");
        final String expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><version>";
        final String expectedJson = "{\"version\":\"";

        Response response = new ResteasyClientBuilder().build().
                target(localUrl("/mapcode/version")).
                request().
                accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertTrue(response.readEntity(String.class).contains(expectedXml));

        response = new ResteasyClientBuilder().build().
                target(localUrl("/mapcode/xml/version")).
                request().
                get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertTrue(response.readEntity(String.class).contains(expectedXml));

        response = new ResteasyClientBuilder().build().
                target(localUrl("/mapcode/json/version")).
                request().
                get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertTrue(response.readEntity(String.class).contains(expectedJson));
    }

    @Nonnull
    private static String localUrl(@Nonnull final String url) {
        return "http://localhost:" + SERVER_PORT + url;
    }
}
