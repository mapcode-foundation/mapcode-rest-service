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
        Assert.assertEquals("{\"total\":533,\"territories\":[{\"aliases\":[\"US\"],\"fullNameAliases\":[\"United States of America\",\"America\"],\"alphaCode\":\"USA\",\"alphaCodeMinimalUnambiguous\":\"USA\",\"alphaCodeMinimal\":\"USA\",\"fullName\":\"USA\",\"alphabets\":[{\"name\":\"ROMAN\"}]},{\"aliases\":[\"IN\"],\"alphaCode\":\"IND\",\"alphaCodeMinimalUnambiguous\":\"IND\",\"alphaCodeMinimal\":\"IND\",\"fullName\":\"India\",\"alphabets\":[{\"name\":\"DEVANAGARI\"},{\"name\":\"BENGALI\"},{\"name\":\"ROMAN\"}]},{\"aliases\":[\"CA\"],\"alphaCode\":\"CAN\",\"alphaCodeMinimalUnambiguous\":\"CAN\",\"alphaCo",
                sub1);
        Assert.assertEquals("\"USA-UM\",\"JTN\"],\"alphaCode\":\"UMI\",\"alphaCodeMinimalUnambiguous\":\"UMI\",\"alphaCodeMinimal\":\"UMI\",\"fullName\":\"United States Minor Outlying Islands\",\"alphabets\":[{\"name\":\"ROMAN\"}]},{\"alphaCode\":\"CPT\",\"alphaCodeMinimalUnambiguous\":\"CPT\",\"alphaCodeMinimal\":\"CPT\",\"fullName\":\"Clipperton Island\",\"alphabets\":[{\"name\":\"ROMAN\"}]},{\"fullNameAliases\":[\"Worldwide\",\"Earth\"],\"alphaCode\":\"AAA\",\"alphaCodeMinimalUnambiguous\":\"AAA\",\"alphaCodeMinimal\":\"AAA\",\"fullName\":\"International\",\"alphabets\":[{\"name\":\"ROMAN\"}]}]}",
                sub2);
    }

    @Test
    public void checkTerritories1XmlJson() {
        LOG.info("checkTerritories1XmlJson");
        final String expectedXml1 = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><territories><total>533</total><territory><alphaCode>USA</alphaCode><alphaCodeMinimalUnambiguous>USA</alphaCodeMinimalUnambiguous><alphaCodeMinimal>USA</alphaCodeMinimal><fullName>USA</fullName><aliases><alias>US</alias></aliases><fullNameAliases><fullNameAlias>United States of America</fullNameAlias><fullNameAlias>America</fullNameAlias></fullNameAliases><alphabets><alphabet><name>ROMAN</name></alphabet></alphabets></territory><territory><a";
        final String expectedXml2 = "sland</fullName><aliases/><fullNameAliases/><alphabets><alphabet><name>ROMAN</name></alphabet></alphabets></territory><territory><alphaCode>AAA</alphaCode><alphaCodeMinimalUnambiguous>AAA</alphaCodeMinimalUnambiguous><alphaCodeMinimal>AAA</alphaCodeMinimal><fullName>International</fullName><aliases/><fullNameAliases><fullNameAlias>Worldwide</fullNameAlias><fullNameAlias>Earth</fullNameAlias></fullNameAliases><alphabets><alphabet><name>ROMAN</name></alphabet></alphabets></territory></territories>";
        final String expectedJson1 = "{\"total\":533,\"territories\":[{\"aliases\":[\"US\"],\"fullNameAliases\":[\"United States of America\",\"America\"],\"alphaCode\":\"USA\",\"alphaCodeMinimalUnambiguous\":\"USA\",\"alphaCodeMinimal\":\"USA\",\"fullName\":\"USA\",\"alphabets\":[{\"name\":\"ROMAN\"}]},{\"aliases\":[\"IN\"],\"alphaCode\":\"IND\",\"alphaCodeMinimalUnambiguous\":\"IND\",\"alphaCodeMinimal\":\"IND\",\"fullName\":\"India\",\"alphabets\":[{\"name\":\"DEVANAGARI\"},{\"name\":\"BENGALI\"},{\"name\":\"ROMAN\"}]},{\"aliases\":[\"CA\"],\"alphaCode\":\"CAN\",\"alphaCodeMinimalUnambiguous\":\"CAN\",\"alphaCo";
        final String expectedJson2 = "\"USA-UM\",\"JTN\"],\"alphaCode\":\"UMI\",\"alphaCodeMinimalUnambiguous\":\"UMI\",\"alphaCodeMinimal\":\"UMI\",\"fullName\":\"United States Minor Outlying Islands\",\"alphabets\":[{\"name\":\"ROMAN\"}]},{\"alphaCode\":\"CPT\",\"alphaCodeMinimalUnambiguous\":\"CPT\",\"alphaCodeMinimal\":\"CPT\",\"fullName\":\"Clipperton Island\",\"alphabets\":[{\"name\":\"ROMAN\"}]},{\"fullNameAliases\":[\"Worldwide\",\"Earth\"],\"alphaCode\":\"AAA\",\"alphaCodeMinimalUnambiguous\":\"AAA\",\"alphaCodeMinimal\":\"AAA\",\"fullName\":\"International\",\"alphabets\":[{\"name\":\"ROMAN\"}]}]}";
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
        Assert.assertEquals(expectedXml1, sub1);
        Assert.assertEquals(expectedXml2, sub2);

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
        Assert.assertEquals(expectedXml1, sub1);
        Assert.assertEquals(expectedXml2, sub2);

        response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/json/territories")).
                request().
                get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        r = response.readEntity(String.class);
        Assert.assertTrue(r.length() > 500);
        sub1 = r.substring(0, 500);
        sub2 = r.substring(r.length() - 500, r.length());
        Assert.assertEquals(expectedJson1, sub1);
        Assert.assertEquals(expectedJson2, sub2);
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
        Assert.assertEquals("{\"total\":533,\"territories\":[{\"fullNameAliases\":[\"Worldwide\",\"Earth\"],\"alphaCode\":\"AAA\",\"alphaCodeMinimalUnambiguous\":\"AAA\",\"alphaCodeMinimal\":\"AAA\",\"fullName\":\"International\",\"alphabets\":[{\"name\":\"ROMAN\"}]}]}",
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
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><territories><total>533</total><territory><alphaCode>AAA</alphaCode><alphaCodeMinimalUnambiguous>AAA</alphaCodeMinimalUnambiguous><alphaCodeMinimal>AAA</alphaCodeMinimal><fullName>International</fullName><aliases/><fullNameAliases><fullNameAlias>Worldwide</fullNameAlias><fullNameAlias>Earth</fullNameAlias></fullNameAliases><alphabets><alphabet><name>ROMAN</name></alphabet></alphabets></territory></territories>",
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
        Assert.assertEquals("{\"alphaCode\":\"NLD\",\"alphaCodeMinimalUnambiguous\":\"NLD\",\"alphaCodeMinimal\":\"NLD\",\"fullName\":\"Netherlands\",\"alphabets\":[{\"name\":\"ROMAN\"}]}",
                response.readEntity(String.class));
    }

    @Test
    public void checkTerritoryJson1() {
        LOG.info("checkTerritoryJson1");
        final String expected = "{\"alphaCode\":\"NLD\",\"alphaCodeMinimalUnambiguous\":\"NLD\",\"alphaCodeMinimal\":\"NLD\",\"fullName\":\"Netherlands\",\"alphabets\":[{\"name\":\"ROMAN\"}]}";
        Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/territories/nld")).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(expected,
                response.readEntity(String.class));

        response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/territories/nld")).
                request().
                get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(expected,
                response.readEntity(String.class));
    }

    @Test
    public void checkTerritoryJson2() {
        LOG.info("checkTerritoryJson2");
        final String expected = "{\"alphaCode\":\"IN-PY\",\"alphaCodeMinimalUnambiguous\":\"PY\",\"alphaCodeMinimal\":\"PY\",\"fullName\":\"Puducherry\",\"parentTerritory\":\"IND\",\"alphabets\":[{\"name\":\"MALAYALAM\"},{\"name\":\"TELUGU\"},{\"name\":\"DEVANAGARI\"}]}";
        final Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/territories/in-py")).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(expected,
                response.readEntity(String.class));
    }

    @Test
    public void checkTerritoryJson3() {
        LOG.info("checkTerritoryJson3");
        final String expected = "{\"fullNameAliases\":[\"Scotland\",\"Great Britain\",\"Northern Ireland\",\"Ireland, Northern\"],\"alphaCode\":\"GBR\",\"alphaCodeMinimalUnambiguous\":\"GBR\",\"alphaCodeMinimal\":\"GBR\",\"fullName\":\"United Kingdom\",\"alphabets\":[{\"name\":\"ROMAN\"}]}";
        final Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/territories/gbr")).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(expected,
                response.readEntity(String.class));
    }

    @Test
    public void checkTerritoryXmlJson1() {
        LOG.info("checkTerritoryXmlJson1");
        final String expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><territory><alphaCode>NLD</alphaCode><alphaCodeMinimalUnambiguous>NLD</alphaCodeMinimalUnambiguous><alphaCodeMinimal>NLD</alphaCodeMinimal><fullName>Netherlands</fullName><aliases/><fullNameAliases/><alphabets><alphabet><name>ROMAN</name></alphabet></alphabets></territory>";
        final String expectedJson = "{\"alphaCode\":\"NLD\",\"alphaCodeMinimalUnambiguous\":\"NLD\",\"alphaCodeMinimal\":\"NLD\",\"fullName\":\"Netherlands\",\"alphabets\":[{\"name\":\"ROMAN\"}]}";
        Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/territories/nld")).
                request().
                accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(expectedXml, response.readEntity(String.class));

        response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/xml/territories/nld")).
                request().
                get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(expectedXml, response.readEntity(String.class));

        response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/json/territories/nld")).
                request().
                get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(expectedJson, response.readEntity(String.class));
    }

    @Test
    public void checkTerritoryXml2() {
        LOG.info("checkTerritoryXml2");
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><territory><alphaCode>IN-PY</alphaCode><alphaCodeMinimalUnambiguous>PY</alphaCodeMinimalUnambiguous><alphaCodeMinimal>PY</alphaCodeMinimal><fullName>Puducherry</fullName><parentTerritory>IND</parentTerritory><aliases/><fullNameAliases/><alphabets><alphabet><name>MALAYALAM</name></alphabet><alphabet><name>TELUGU</name></alphabet><alphabet><name>DEVANAGARI</name></alphabet></alphabets></territory>";
        final Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/territories/in-py")).
                request().
                accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(expected,
                response.readEntity(String.class));
    }

    @Test
    public void checkTerritoryXml3() {
        LOG.info("checkTerritoryXml3");
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><territory><alphaCode>GBR</alphaCode><alphaCodeMinimalUnambiguous>GBR</alphaCodeMinimalUnambiguous><alphaCodeMinimal>GBR</alphaCodeMinimal><fullName>United Kingdom</fullName><aliases/><fullNameAliases><fullNameAlias>Scotland</fullNameAlias><fullNameAlias>Great Britain</fullNameAlias><fullNameAlias>Northern Ireland</fullNameAlias><fullNameAlias>Ireland, Northern</fullNameAlias></fullNameAliases><alphabets><alphabet><name>ROMAN</name></alphabet></alphabets></territory>";
        final Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/territories/gbr")).
                request().
                accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(expected,
                response.readEntity(String.class));
    }

    @Test
    public void checkTerritoryStateJson() {
        LOG.info("checkTerritoryStateJson");
        final String usIn = "{\"alphaCode\":\"US-IN\",\"alphaCodeMinimalUnambiguous\":\"US-IN\",\"alphaCodeMinimal\":\"IN\",\"fullName\":\"Indiana\",\"parentTerritory\":\"USA\",\"alphabets\":[{\"name\":\"ROMAN\"}]}";
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
        Assert.assertEquals("{\"alphaCode\":\"RU-IN\",\"alphaCodeMinimalUnambiguous\":\"RU-IN\",\"alphaCodeMinimal\":\"IN\",\"fullName\":\"Ingushetia Republic\",\"parentTerritory\":\"RUS\",\"alphabets\":[{\"name\":\"CYRILLIC\"},{\"name\":\"ROMAN\"}]}",
                response.readEntity(String.class));
    }
}
