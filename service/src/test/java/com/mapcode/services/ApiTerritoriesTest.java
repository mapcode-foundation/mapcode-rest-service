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
public class ApiTerritoriesTest {
    private static final Logger LOG = LoggerFactory.getLogger(ApiTerritoriesTest.class);

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
    public void checkTerritories1Json() {
        LOG.info("checkTerritories1Json");
        final Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/territories")).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        final String r = response.readEntity(String.class);
        Assert.assertTrue(r.length() > 500);
        final String sub1 = r.substring(0, 500);
        final String sub2 = r.substring(r.length() - 500, r.length());
        Assert.assertEquals("{\"total\":533,\"territories\":[{\"aliases\":[\"US\"],\"fullNameAliases\":[\"United States of America\",\"America\"],\"alphaCode\":\"USA\",\"alphaCodeMinimalUnambiguous\":\"USA\",\"alphaCodeMinimal\":\"USA\",\"fullName\":\"USA\"},{\"aliases\":[\"IN\"],\"alphaCode\":\"IND\",\"alphaCodeMinimalUnambiguous\":\"IND\",\"alphaCodeMinimal\":\"IND\",\"fullName\":\"India\"},{\"aliases\":[\"CA\"],\"alphaCode\":\"CAN\",\"alphaCodeMinimalUnambiguous\":\"CAN\",\"alphaCodeMinimal\":\"CAN\",\"fullName\":\"Canada\"},{\"aliases\":[\"AU\"],\"alphaCode\":\"AUS\",\"alphaCodeMinimalUnambiguous\"",
                sub1);
        Assert.assertEquals("aCodeMinimal\":\"XJ\",\"fullName\":\"Xinjiang Uyghur\",\"parentTerritory\":\"CHN\"},{\"aliases\":[\"US-UM\",\"USA-UM\",\"JTN\"],\"alphaCode\":\"UMI\",\"alphaCodeMinimalUnambiguous\":\"UMI\",\"alphaCodeMinimal\":\"UMI\",\"fullName\":\"United States Minor Outlying Islands\"},{\"alphaCode\":\"CPT\",\"alphaCodeMinimalUnambiguous\":\"CPT\",\"alphaCodeMinimal\":\"CPT\",\"fullName\":\"Clipperton Island\"},{\"fullNameAliases\":[\"Worldwide\",\"Earth\"],\"alphaCode\":\"AAA\",\"alphaCodeMinimalUnambiguous\":\"AAA\",\"alphaCodeMinimal\":\"AAA\",\"fullName\":\"International\"}]}",
                sub2);
    }

    @Test
    public void checkTerritories1Xml() {
        LOG.info("checkTerritories1Xml");
        final String expected1 = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><territories><total>533</total><territory><alphaCode>USA</alphaCode><alphaCodeMinimalUnambiguous>USA</alphaCodeMinimalUnambiguous><alphaCodeMinimal>USA</alphaCodeMinimal><fullName>USA</fullName><aliases><alias>US</alias></aliases><fullNameAliases><fullNameAlias>United States of America</fullNameAlias><fullNameAlias>America</fullNameAlias></fullNameAliases></territory><territory><alphaCode>IND</alphaCode><alphaCodeMinimalUnambiguous>IND</alph";
        final String expected2 = "<alphaCodeMinimalUnambiguous>CPT</alphaCodeMinimalUnambiguous><alphaCodeMinimal>CPT</alphaCodeMinimal><fullName>Clipperton Island</fullName><aliases/><fullNameAliases/></territory><territory><alphaCode>AAA</alphaCode><alphaCodeMinimalUnambiguous>AAA</alphaCodeMinimalUnambiguous><alphaCodeMinimal>AAA</alphaCodeMinimal><fullName>International</fullName><aliases/><fullNameAliases><fullNameAlias>Worldwide</fullNameAlias><fullNameAlias>Earth</fullNameAlias></fullNameAliases></territory></territories>";
        Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/territories")).
                request().
                accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        String r = response.readEntity(String.class);
        Assert.assertTrue(r.length() > 500);
        String sub1 = r.substring(0, 500);
        String sub2 = r.substring(r.length() - 500, r.length());
        Assert.assertEquals(expected1,
                sub1);
        Assert.assertEquals(expected2,
                sub2);

        response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/xml/territories")).
                request().
                get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        r = response.readEntity(String.class);
        Assert.assertTrue(r.length() > 500);
        sub1 = r.substring(0, 500);
        sub2 = r.substring(r.length() - 500, r.length());
        Assert.assertEquals(expected1,
                sub1);
        Assert.assertEquals(expected2,
                sub2);
    }

    @Test
    public void checkTerritoriesCountJsonError() {
        LOG.info("checkTerritoriesCountJsonError");
        final Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/territories?count=-1")).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(400, response.getStatus());
    }

    @Test
    public void checkTerritories2Json() {
        LOG.info("checkTerritories2Json");
        final Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/territories?count=1&offset=-1")).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        final String r = response.readEntity(String.class);
        Assert.assertEquals("{\"total\":533,\"territories\":[{\"fullNameAliases\":[\"Worldwide\",\"Earth\"],\"alphaCode\":\"AAA\",\"alphaCodeMinimalUnambiguous\":\"AAA\",\"alphaCodeMinimal\":\"AAA\",\"fullName\":\"International\"}]}",
                r);
    }

    @Test
    public void checkTerritories2Xml() {
        LOG.info("checkTerritories2Xml");
        final Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/territories?count=1&offset=-1")).
                request().
                accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        final String r = response.readEntity(String.class);
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><territories><total>533</total><territory><alphaCode>AAA</alphaCode><alphaCodeMinimalUnambiguous>AAA</alphaCodeMinimalUnambiguous><alphaCodeMinimal>AAA</alphaCodeMinimal><fullName>International</fullName><aliases/><fullNameAliases><fullNameAlias>Worldwide</fullNameAlias><fullNameAlias>Earth</fullNameAlias></fullNameAliases></territory></territories>",
                r);
    }

    @Test
    public void checkTerritoryJsonError() {
        LOG.info("checkTerritoryJsonError");
        final Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/territories/xyz")).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(400, response.getStatus());
    }

    @Test
    public void checkTerritoryJson() {
        LOG.info("checkTerritoryJson");
        final Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/territories/nld")).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"alphaCode\":\"NLD\",\"alphaCodeMinimalUnambiguous\":\"NLD\",\"alphaCodeMinimal\":\"NLD\",\"fullName\":\"Netherlands\"}",
                response.readEntity(String.class));
    }

    @Test
    public void checkTerritoryXml() {
        LOG.info("checkTerritoryXml");
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><territory><alphaCode>NLD</alphaCode><alphaCodeMinimalUnambiguous>NLD</alphaCodeMinimalUnambiguous><alphaCodeMinimal>NLD</alphaCodeMinimal><fullName>Netherlands</fullName><aliases/><fullNameAliases/></territory>";
        Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/territories/nld")).
                request().
                accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(expected,
                response.readEntity(String.class));

        response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/xml/territories/nld")).
                request().
                get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(expected,
                response.readEntity(String.class));
    }

    @Test
    public void checkTerritoryStateJson() {
        LOG.info("checkTerritoryStateJson");
        final String usIn = "{\"alphaCode\":\"US-IN\",\"alphaCodeMinimalUnambiguous\":\"US-IN\",\"alphaCodeMinimal\":\"IN\",\"fullName\":\"Indiana\",\"parentTerritory\":\"USA\"}";
        Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/territories/in")).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(usIn, response.readEntity(String.class));

        response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/territories/in?context=xyz")).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(400, response.getStatus());

        response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/territories/in?context=nld")).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(400, response.getStatus());

        response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/territories/in?context=ind")).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(usIn, response.readEntity(String.class));

        response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/territories/in?context=us")).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(usIn, response.readEntity(String.class));

        response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/territories/in?context=ru")).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"alphaCode\":\"RU-IN\",\"alphaCodeMinimalUnambiguous\":\"RU-IN\",\"alphaCodeMinimal\":\"IN\",\"fullName\":\"Ingushetia Republic\",\"parentTerritory\":\"RUS\"}",
                response.readEntity(String.class));
    }
}
