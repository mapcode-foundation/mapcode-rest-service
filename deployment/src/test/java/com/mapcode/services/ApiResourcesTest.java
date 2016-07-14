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

import com.mapcode.Alphabet;
import com.mapcode.services.implementation.MapcodeResourceImpl;
import com.mapcode.services.implementation.RootResourceImpl;
import com.mapcode.services.implementation.SystemMetricsImpl;
import com.tomtom.speedtools.maven.MavenProperties;
import com.tomtom.speedtools.rest.Reactor;
import com.tomtom.speedtools.rest.ResourceProcessor;
import com.tomtom.speedtools.testutils.akka.SimpleExecutionContext;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.plugins.server.tjws.TJWSEmbeddedJaxrsServer;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.ExecutionContext;

import javax.annotation.Nonnull;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@SuppressWarnings("JUnitTestMethodWithNoAssertions")
public class ApiResourcesTest {
    private static final Logger LOG = LoggerFactory.getLogger(ApiResourcesTest.class);

    private static final String TEST_POM_VERSION = "1.0.0-TEST";

    private static final Double TEST_LAT1 = 50.141706;
    private static final Double TEST_LON1 = 6.135864;
    private static final String TEST_LATLON1 = TEST_LAT1 + "," + TEST_LON1;

    private static final Double TEST_LAT2 = 52.159853;
    private static final Double TEST_LON2 = 4.499790;
    private static final String TEST_LATLON2 = TEST_LAT2 + "," + TEST_LON2;

    private static final String TEST_CODE1 = "VJ0L6.9PNQ";
    private static final String TEST_CODE2 = "JL0.KP";
    private static final String TEST_CONTEXT2 = "LUX";

    private static final int PORT = 8081;
    private static final String HOST = "http://localhost:" + PORT;

    private TJWSEmbeddedJaxrsServer server;

    @Before
    public void startServer() {
        server = new TJWSEmbeddedJaxrsServer();
        server.setPort(PORT);

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

            // This method is stubbed and never used.
            @Nonnull
            @Override
            public <T> T createTopLevelActor(@Nonnull final Class<T> interfaceClass, @Nonnull final Class<? extends T> implementationClass, @Nonnull Object... explicitParameters) {
                assert false;
                return null;
            }
        };
        final ResourceProcessor resourceProcessor = new ResourceProcessor(reactor);

        final MavenProperties mavenProperties = new MavenProperties(TEST_POM_VERSION);
        final SystemMetricsImpl metrics = new SystemMetricsImpl();

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
        server.start();
    }

    @After
    public void stopServer() {
        server.stop();
    }

    @Test
    public void checkStatus() {
        LOG.info("checkStatus");
        final Response r = new ResteasyClientBuilder().build().
                target(HOST + "/mapcode/version").
                request().
                get();
        Assert.assertNotNull(r);
        final int status = r.getStatus();
        LOG.info("status = {}", status);
        Assert.assertEquals(200, status);
    }

    @Test
    public void checkVersionJson() {
        LOG.info("checkVersionJson");
        final Response response = new ResteasyClientBuilder().build().
                target(HOST + "/mapcode/version").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"version\":\"1.0.0-TEST\"}",
                response.readEntity(String.class));
    }

    @Test
    public void checkVersionXml() {
        LOG.info("checkVersionXml");
        final Response response = new ResteasyClientBuilder().build().
                target(HOST + "/mapcode/version").
                request().
                accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><version><version>1.0.0-TEST</version></version>",
                response.readEntity(String.class));
    }

    @Test
    public void checkCodesJson() {
        LOG.info("checkCodesJson");
        final Response response = new ResteasyClientBuilder().build().
                target(HOST + "/mapcode/codes/" + TEST_LATLON1).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"international\":{\"mapcode\":\"VJ0L6.9PNQ\"},\"mapcodes\":[{\"mapcode\":\"JL0.KP\",\"territory\":\"LUX\"},{\"mapcode\":\"R8RN.07Z\",\"territory\":\"LUX\"},{\"mapcode\":\"SQB.NR3\",\"territory\":\"BEL\"},{\"mapcode\":\"R8RN.07Z\",\"territory\":\"BEL\"},{\"mapcode\":\"0L46.LG9\",\"territory\":\"DEU\"},{\"mapcode\":\"R8RN.07Z\",\"territory\":\"FRA\"},{\"mapcode\":\"VJ0L6.9PNQ\"}]}",
                response.readEntity(String.class));
    }

    @Test
    public void checkCodesXml() {
        LOG.info("checkCodesXml");
        final Response response = new ResteasyClientBuilder().build().
                target(HOST + "/mapcode/codes/" + TEST_LATLON1).
                request().
                accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcodes><international><mapcode>VJ0L6.9PNQ</mapcode></international><mapcodes><mapcode><mapcode>JL0.KP</mapcode><territory>LUX</territory></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><territory>LUX</territory></mapcode><mapcode><mapcode>SQB.NR3</mapcode><territory>BEL</territory></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><territory>BEL</territory></mapcode><mapcode><mapcode>0L46.LG9</mapcode><territory>DEU</territory></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><territory>FRA</territory></mapcode><mapcode><mapcode>VJ0L6.9PNQ</mapcode></mapcode></mapcodes></mapcodes>",
                response.readEntity(String.class));
    }

    @Test
    public void checkCodesPrecision0Json() {
        LOG.info("checkCodesPrecision0Json");
        final Response response = new ResteasyClientBuilder().build().
                target(HOST + "/mapcode/codes/" + TEST_LATLON1 + "?precision=0").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"international\":{\"mapcode\":\"VJ0L6.9PNQ\"},\"mapcodes\":[{\"mapcode\":\"JL0.KP\",\"territory\":\"LUX\"},{\"mapcode\":\"R8RN.07Z\",\"territory\":\"LUX\"},{\"mapcode\":\"SQB.NR3\",\"territory\":\"BEL\"},{\"mapcode\":\"R8RN.07Z\",\"territory\":\"BEL\"},{\"mapcode\":\"0L46.LG9\",\"territory\":\"DEU\"},{\"mapcode\":\"R8RN.07Z\",\"territory\":\"FRA\"},{\"mapcode\":\"VJ0L6.9PNQ\"}]}",
                response.readEntity(String.class));
    }

    @Test
    public void checkCodesPrecision0Xml() {
        LOG.info("checkCodesPrecision0Xml");
        final Response response = new ResteasyClientBuilder().build().
                target(HOST + "/mapcode/codes/" + TEST_LATLON1 + "?precision=0").
                request().
                accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcodes><international><mapcode>VJ0L6.9PNQ</mapcode></international><mapcodes><mapcode><mapcode>JL0.KP</mapcode><territory>LUX</territory></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><territory>LUX</territory></mapcode><mapcode><mapcode>SQB.NR3</mapcode><territory>BEL</territory></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><territory>BEL</territory></mapcode><mapcode><mapcode>0L46.LG9</mapcode><territory>DEU</territory></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><territory>FRA</territory></mapcode><mapcode><mapcode>VJ0L6.9PNQ</mapcode></mapcode></mapcodes></mapcodes>",
                response.readEntity(String.class));
    }

    @Test
    public void checkCodesPrecision1Json() {
        LOG.info("checkCodesPrecision1Json");
        final Response response = new ResteasyClientBuilder().build().
                target(HOST + "/mapcode/codes/" + TEST_LATLON1 + "?precision=1").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"international\":{\"mapcode\":\"VJ0L6.9PNQ-0\"},\"mapcodes\":[{\"mapcode\":\"JL0.KP-8\",\"territory\":\"LUX\"},{\"mapcode\":\"R8RN.07Z-M\",\"territory\":\"LUX\"},{\"mapcode\":\"SQB.NR3-P\",\"territory\":\"BEL\"},{\"mapcode\":\"R8RN.07Z-M\",\"territory\":\"BEL\"},{\"mapcode\":\"0L46.LG9-Q\",\"territory\":\"DEU\"},{\"mapcode\":\"R8RN.07Z-M\",\"territory\":\"FRA\"},{\"mapcode\":\"VJ0L6.9PNQ-0\"}]}",
                response.readEntity(String.class));
    }

    @Test
    public void checkCodesPrecision1Xml() {
        LOG.info("checkCodesPrecision1Xml");
        final Response response = new ResteasyClientBuilder().build().
                target(HOST + "/mapcode/codes/" + TEST_LATLON1 + "?precision=1").
                request().
                accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcodes><international><mapcode>VJ0L6.9PNQ-0</mapcode></international><mapcodes><mapcode><mapcode>JL0.KP-8</mapcode><territory>LUX</territory></mapcode><mapcode><mapcode>R8RN.07Z-M</mapcode><territory>LUX</territory></mapcode><mapcode><mapcode>SQB.NR3-P</mapcode><territory>BEL</territory></mapcode><mapcode><mapcode>R8RN.07Z-M</mapcode><territory>BEL</territory></mapcode><mapcode><mapcode>0L46.LG9-Q</mapcode><territory>DEU</territory></mapcode><mapcode><mapcode>R8RN.07Z-M</mapcode><territory>FRA</territory></mapcode><mapcode><mapcode>VJ0L6.9PNQ-0</mapcode></mapcode></mapcodes></mapcodes>",
                response.readEntity(String.class));
    }

    @Test
    public void checkCodesPrecision8Json() {
        LOG.info("checkCodesPrecision8Json");
        final Response response = new ResteasyClientBuilder().build().
                target(HOST + "/mapcode/codes/" + TEST_LATLON1 + "?precision=8").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"international\":{\"mapcode\":\"VJ0L6.9PNQ-03Q7CGV4\"},\"mapcodes\":[{\"mapcode\":\"JL0.KP-81B34315\",\"territory\":\"LUX\"},{\"mapcode\":\"R8RN.07Z-MWPCRQBK\",\"territory\":\"LUX\"},{\"mapcode\":\"SQB.NR3-P6880000\",\"territory\":\"BEL\"},{\"mapcode\":\"R8RN.07Z-MWPCRQBK\",\"territory\":\"BEL\"},{\"mapcode\":\"0L46.LG9-QWPVQVRW\",\"territory\":\"DEU\"},{\"mapcode\":\"R8RN.07Z-MWPCRQBK\",\"territory\":\"FRA\"},{\"mapcode\":\"VJ0L6.9PNQ-03Q7CGV4\"}]}",
                response.readEntity(String.class));
    }

    @Test
    public void checkCodesPrecision8Xml() {
        LOG.info("checkCodesPrecision8Xml");
        final Response response = new ResteasyClientBuilder().build().
                target(HOST + "/mapcode/codes/" + TEST_LATLON1 + "?precision=8").
                request().
                accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcodes><international><mapcode>VJ0L6.9PNQ-03Q7CGV4</mapcode></international><mapcodes><mapcode><mapcode>JL0.KP-81B34315</mapcode><territory>LUX</territory></mapcode><mapcode><mapcode>R8RN.07Z-MWPCRQBK</mapcode><territory>LUX</territory></mapcode><mapcode><mapcode>SQB.NR3-P6880000</mapcode><territory>BEL</territory></mapcode><mapcode><mapcode>R8RN.07Z-MWPCRQBK</mapcode><territory>BEL</territory></mapcode><mapcode><mapcode>0L46.LG9-QWPVQVRW</mapcode><territory>DEU</territory></mapcode><mapcode><mapcode>R8RN.07Z-MWPCRQBK</mapcode><territory>FRA</territory></mapcode><mapcode><mapcode>VJ0L6.9PNQ-03Q7CGV4</mapcode></mapcode></mapcodes></mapcodes>",
                response.readEntity(String.class));
    }

    @Test
    public void checkCodesLocalJson() {
        LOG.info("checkCodesLocalJson");
        final Response response = new ResteasyClientBuilder().build().
                target(HOST + "/mapcode/codes/" + TEST_LATLON2 + "/local").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"mapcode\":\"QKM.N4\",\"territory\":\"NLD\"}",
                response.readEntity(String.class));
    }

    @Test
    public void checkCodesLocalXml() {
        LOG.info("checkCodesLocalXml");
        final Response response = new ResteasyClientBuilder().build().
                target(HOST + "/mapcode/codes/" + TEST_LATLON2 + "/local").
                request().
                accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcode><mapcode>QKM.N4</mapcode><territory>NLD</territory></mapcode>",
                response.readEntity(String.class));
    }

    @Test
    public void checkCodesInternationalJson() {
        LOG.info("checkCodesInternationalJson");
        final Response response = new ResteasyClientBuilder().build().
                target(HOST + "/mapcode/codes/" + TEST_LATLON1 + "/International").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"mapcode\":\"VJ0L6.9PNQ\"}",
                response.readEntity(String.class));
    }

    @Test
    public void checkCodesInternationalXml() {
        LOG.info("checkCodesInternationalXml");
        final Response response = new ResteasyClientBuilder().build().
                target(HOST + "/mapcode/codes/" + TEST_LATLON1 + "/International").
                request().
                accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcode><mapcode>VJ0L6.9PNQ</mapcode></mapcode>",
                response.readEntity(String.class));
    }

    @Test
    public void checkCodesIncludeJson() {
        LOG.info("checkCodesIncludeJson");
        final Response response = new ResteasyClientBuilder().build().
                target(HOST + "/mapcode/codes/" + TEST_LATLON2 + "?include=offset,territory,alphabet").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"local\":{\"mapcode\":\"QKM.N4\",\"mapcodeInAlphabet\":\"QKM.N4\",\"territory\":\"NLD\",\"territoryInAlphabet\":\"NLD\",\"offsetMeters\":2.843693},\"international\":{\"mapcode\":\"VHVN4.YZ74\",\"mapcodeInAlphabet\":\"VHVN4.YZ74\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"AAA\",\"offsetMeters\":1.907245},\"mapcodes\":[{\"mapcode\":\"QKM.N4\",\"mapcodeInAlphabet\":\"QKM.N4\",\"territory\":\"NLD\",\"territoryInAlphabet\":\"NLD\",\"offsetMeters\":2.843693},{\"mapcode\":\"CZQ.376\",\"mapcodeInAlphabet\":\"CZQ.376\",\"territory\":\"NLD\",\"territoryInAlphabet\":\"NLD\",\"offsetMeters\":5.004936},{\"mapcode\":\"N39J.QW0\",\"mapcodeInAlphabet\":\"N39J.QW0\",\"territory\":\"NLD\",\"territoryInAlphabet\":\"NLD\",\"offsetMeters\":2.836538},{\"mapcode\":\"VHVN4.YZ74\",\"mapcodeInAlphabet\":\"VHVN4.YZ74\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"AAA\",\"offsetMeters\":1.907245}]}",
                response.readEntity(String.class));
    }

    @Test
    public void checkCodesIncludeXml() {
        LOG.info("checkCodesIncludeXml");
        final Response response = new ResteasyClientBuilder().build().
                target(HOST + "/mapcode/codes/" + TEST_LATLON2 + "?include=offset,territory,alphabet").
                request().
                accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcodes><local><mapcode>QKM.N4</mapcode><mapcodeInAlphabet>QKM.N4</mapcodeInAlphabet><territory>NLD</territory><territoryInAlphabet>NLD</territoryInAlphabet><offsetMeters>2.843693</offsetMeters></local><international><mapcode>VHVN4.YZ74</mapcode><mapcodeInAlphabet>VHVN4.YZ74</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>AAA</territoryInAlphabet><offsetMeters>1.907245</offsetMeters></international><mapcodes><mapcode><mapcode>QKM.N4</mapcode><mapcodeInAlphabet>QKM.N4</mapcodeInAlphabet><territory>NLD</territory><territoryInAlphabet>NLD</territoryInAlphabet><offsetMeters>2.843693</offsetMeters></mapcode><mapcode><mapcode>CZQ.376</mapcode><mapcodeInAlphabet>CZQ.376</mapcodeInAlphabet><territory>NLD</territory><territoryInAlphabet>NLD</territoryInAlphabet><offsetMeters>5.004936</offsetMeters></mapcode><mapcode><mapcode>N39J.QW0</mapcode><mapcodeInAlphabet>N39J.QW0</mapcodeInAlphabet><territory>NLD</territory><territoryInAlphabet>NLD</territoryInAlphabet><offsetMeters>2.836538</offsetMeters></mapcode><mapcode><mapcode>VHVN4.YZ74</mapcode><mapcodeInAlphabet>VHVN4.YZ74</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>AAA</territoryInAlphabet><offsetMeters>1.907245</offsetMeters></mapcode></mapcodes></mapcodes>",
                response.readEntity(String.class));
    }

    private static void doCheckJson(@Nonnull final String alphabet, @Nonnull final String expected) {
        final Response response = new ResteasyClientBuilder().build().
                target(HOST + "/mapcode/codes/" + TEST_LATLON1 + "?include=territory,alphabet&alphabet=" + alphabet).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(expected, response.readEntity(String.class));
    }

    @Test
    public void checkCoords1Json() {
        LOG.info("checkCoords1Json");
        final Response response = new ResteasyClientBuilder().build().
                target(HOST + "/mapcode/coords/" + TEST_CODE1).
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
                target(HOST + "/mapcode/coords/" + TEST_CODE1).
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
                target(HOST + "/mapcode/coords/" + TEST_CODE2 + "?context=" + TEST_CONTEXT2).
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
                target(HOST + "/mapcode/coords/" + TEST_CODE2 + "?context=" + TEST_CONTEXT2).
                request().
                accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><point><latDeg>50.141735</latDeg><lonDeg>6.135845</lonDeg></point>",
                response.readEntity(String.class));
    }

    @Test
    public void checkTerritoriesJson() {
        LOG.info("checkTerritoriesJson");
        final Response response = new ResteasyClientBuilder().build().
                target(HOST + "/mapcode/territories").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        final String r = response.readEntity(String.class);
        Assert.assertTrue(r.length() > 500);
        final String sub1 = r.substring(0, 500);
        final String sub2 = r.substring(r.length() - 500, r.length());
        Assert.assertEquals("[{\"aliases\":[\"US\"],\"fullNameAliases\":[\"United States of America\",\"America\"],\"alphaCode\":\"USA\",\"alphaCodeMinimalUnambiguous\":\"USA\",\"alphaCodeMinimal\":\"USA\",\"fullName\":\"USA\"},{\"aliases\":[\"IN\"],\"alphaCode\":\"IND\",\"alphaCodeMinimalUnambiguous\":\"IND\",\"alphaCodeMinimal\":\"IND\",\"fullName\":\"India\"},{\"aliases\":[\"CA\"],\"alphaCode\":\"CAN\",\"alphaCodeMinimalUnambiguous\":\"CAN\",\"alphaCodeMinimal\":\"CAN\",\"fullName\":\"Canada\"},{\"aliases\":[\"AU\"],\"alphaCode\":\"AUS\",\"alphaCodeMinimalUnambiguous\":\"AUS\",\"alphaCodeMinimal\":\"",
                sub1);
        Assert.assertEquals("haCodeMinimal\":\"XJ\",\"fullName\":\"Xinjiang Uyghur\",\"parentTerritory\":\"CHN\"},{\"aliases\":[\"US-UM\",\"USA-UM\",\"JTN\"],\"alphaCode\":\"UMI\",\"alphaCodeMinimalUnambiguous\":\"UMI\",\"alphaCodeMinimal\":\"UMI\",\"fullName\":\"United States Minor Outlying Islands\"},{\"alphaCode\":\"CPT\",\"alphaCodeMinimalUnambiguous\":\"CPT\",\"alphaCodeMinimal\":\"CPT\",\"fullName\":\"Clipperton Island\"},{\"fullNameAliases\":[\"Worldwide\",\"Earth\"],\"alphaCode\":\"AAA\",\"alphaCodeMinimalUnambiguous\":\"AAA\",\"alphaCodeMinimal\":\"AAA\",\"fullName\":\"International\"}]",
                sub2);
    }

    @Test
    public void checkTerritoriesXml() {
        LOG.info("checkTerritoriesXml");
        final Response response = new ResteasyClientBuilder().build().
                target(HOST + "/mapcode/territories").
                request().
                accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        final String r = response.readEntity(String.class);
        Assert.assertTrue(r.length() > 500);
        final String sub1 = r.substring(0, 500);
        final String sub2 = r.substring(r.length() - 500, r.length());
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><territories><territory><alphaCode>USA</alphaCode><alphaCodeMinimalUnambiguous>USA</alphaCodeMinimalUnambiguous><alphaCodeMinimal>USA</alphaCodeMinimal><fullName>USA</fullName><aliases><alias>US</alias></aliases><fullNameAliases><fullNameAlias>United States of America</fullNameAlias><fullNameAlias>America</fullNameAlias></fullNameAliases></territory><territory><alphaCode>IND</alphaCode><alphaCodeMinimalUnambiguous>IND</alphaCodeMinimalUnambi",
                sub1);
        Assert.assertEquals("<alphaCodeMinimalUnambiguous>CPT</alphaCodeMinimalUnambiguous><alphaCodeMinimal>CPT</alphaCodeMinimal><fullName>Clipperton Island</fullName><aliases/><fullNameAliases/></territory><territory><alphaCode>AAA</alphaCode><alphaCodeMinimalUnambiguous>AAA</alphaCodeMinimalUnambiguous><alphaCodeMinimal>AAA</alphaCodeMinimal><fullName>International</fullName><aliases/><fullNameAliases><fullNameAlias>Worldwide</fullNameAlias><fullNameAlias>Earth</fullNameAlias></fullNameAliases></territory></territories>",
                sub2);
    }

    @Test
    public void checkTerritoryJson() {
        LOG.info("checkTerritoryJson");
        final Response response = new ResteasyClientBuilder().build().
                target(HOST + "/mapcode/territories/nld").
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
        final Response response = new ResteasyClientBuilder().build().
                target(HOST + "/mapcode/territories/nld").
                request().
                accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><territory><alphaCode>NLD</alphaCode><alphaCodeMinimalUnambiguous>NLD</alphaCodeMinimalUnambiguous><alphaCodeMinimal>NLD</alphaCodeMinimal><fullName>Netherlands</fullName><aliases/><fullNameAliases/></territory>",
                response.readEntity(String.class));
    }

    @Test
    public void checkAlphabetsJson() {
        LOG.info("checkAlphabetsJson");
        final Response response = new ResteasyClientBuilder().build().
                target(HOST + "/mapcode/alphabets").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("[{\"name\":\"ROMAN\"},{\"name\":\"GREEK\"},{\"name\":\"CYRILLIC\"},{\"name\":\"HEBREW\"},{\"name\":\"HINDI\"},{\"name\":\"MALAY\"},{\"name\":\"GEORGIAN\"},{\"name\":\"KATAKANA\"},{\"name\":\"THAI\"},{\"name\":\"LAO\"},{\"name\":\"ARMENIAN\"},{\"name\":\"BENGALI\"},{\"name\":\"GURMUKHI\"},{\"name\":\"TIBETAN\"}]",
                response.readEntity(String.class));
    }

    @Test
    public void checkAlphabetsCountJson() {
        LOG.info("checkAlphabetsCountJson");
        final Response response = new ResteasyClientBuilder().build().
                target(HOST + "/mapcode/alphabets?count=2").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("[{\"name\":\"ROMAN\"},{\"name\":\"GREEK\"}]",
                response.readEntity(String.class));
    }

    @Test
    public void checkAlphabetsCountXml() {
        LOG.info("checkAlphabetsCountXml");
        final Response response = new ResteasyClientBuilder().build().
                target(HOST + "/mapcode/alphabets?count=2").
                request().
                accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><alphabets><alphabet><name>ROMAN</name></alphabet><alphabet><name>GREEK</name></alphabet></alphabets>",
                response.readEntity(String.class));
    }

    @Test
    public void checkAlphabetsCountOffsetJson() {
        LOG.info("checkAlphabetsCountOffsetJson");
        final Response response = new ResteasyClientBuilder().build().
                target(HOST + "/mapcode/alphabets?count=1&offset=1").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("[{\"name\":\"GREEK\"}]",
                response.readEntity(String.class));
    }

    @Test
    public void checkAlphabetsCountOffsetXml() {
        LOG.info("checkAlphabetsCountOffsetXml");
        final Response response = new ResteasyClientBuilder().build().
                target(HOST + "/mapcode/alphabets?count=1&offset=1").
                request().
                accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><alphabets><alphabet><name>GREEK</name></alphabet></alphabets>",
                response.readEntity(String.class));
    }

    @Test
    public void checkAlphabetsCountOffsetFromEndJson() {
        LOG.info("checkAlphabetsCountOffsetFromEndJson");
        final Response response = new ResteasyClientBuilder().build().
                target(HOST + "/mapcode/alphabets?count=1&offset=-1").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("[{\"name\":\"TIBETAN\"}]",
                response.readEntity(String.class));
    }

    @Test
    public void checkAlphabetsCountOffsetFromEndXml() {
        LOG.info("checkAlphabetsCountOffsetXml");
        final Response response = new ResteasyClientBuilder().build().
                target(HOST + "/mapcode/alphabets?count=1&offset=-1").
                request().
                accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><alphabets><alphabet><name>TIBETAN</name></alphabet></alphabets>",
                response.readEntity(String.class));
    }

    @Test
    public void checkAlphabetJson() {
        LOG.info("checkAlphabetJson");
        final Response response = new ResteasyClientBuilder().build().
                target(HOST + "/mapcode/alphabets/greek").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"name\":\"GREEK\"}",
                response.readEntity(String.class));
    }

    @Test
    public void checkAlphabetXml() {
        LOG.info("checkAlphabetXml");
        final Response response = new ResteasyClientBuilder().build().
                target(HOST + "/mapcode/alphabets/greek").
                request().
                accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><alphabet><name>GREEK</name></alphabet>",
                response.readEntity(String.class));
    }

    @Test
    public void checkAllAlphabetsJson() {
        LOG.info("checkAlphabetsJson");
        int i = 0;
        for (final Alphabet alphabet : Alphabet.values()) {
            doCheckAlphabet(alphabet.name(), MediaType.APPLICATION_JSON_TYPE, EXPECTED_ALPHABETS_JSON[i]);
            ++i;
        }
    }

    @Test
    public void checkAllAlphabetsXml() {
        LOG.info("checkAlphabetsXml");
        int i = 0;
        for (final Alphabet alphabet : Alphabet.values()) {
            doCheckAlphabet(alphabet.name(), MediaType.APPLICATION_XML_TYPE, EXPECTED_ALPHABETS_XML[i]);
            ++i;
        }
    }

    private static void doCheckAlphabet(
            @Nonnull final String alphabet,
            @Nonnull final MediaType mediaType,
            @Nonnull final String expected) {
        LOG.info("doCheckAlphabet: alphabet={}, mediaType={}", alphabet, mediaType);
        final Response response = new ResteasyClientBuilder().build().
                target(HOST + "/mapcode/codes/" + TEST_LATLON1 + "?include=territory,alphabet&alphabet=" + alphabet).
                request().
                accept(mediaType).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(expected, response.readEntity(String.class));
    }

    private final static String[] EXPECTED_ALPHABETS_JSON = {
            "{\"international\":{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"VJ0L6.9PNQ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"AAA\"},\"mapcodes\":[{\"mapcode\":\"JL0.KP\",\"mapcodeInAlphabet\":\"JL0.KP\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"LUX\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"R8RN.07Z\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"LUX\"},{\"mapcode\":\"SQB.NR3\",\"mapcodeInAlphabet\":\"SQB.NR3\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"BEL\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"R8RN.07Z\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"BEL\"},{\"mapcode\":\"0L46.LG9\",\"mapcodeInAlphabet\":\"0L46.LG9\",\"territory\":\"DEU\",\"territoryInAlphabet\":\"DEU\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"R8RN.07Z\",\"territory\":\"FRA\",\"territoryInAlphabet\":\"FRA\"},{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"VJ0L6.9PNQ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"AAA\"}]}",
            "{\"international\":{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"ΦΠ0Λ6.9ΡΝΘ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"ΑΑΑ\"},\"mapcodes\":[{\"mapcode\":\"JL0.KP\",\"mapcodeInAlphabet\":\"ΠΛ0.ΚΡ\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"LUX\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"Ψ8ΨΝ.07Ζ\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"LUX\"},{\"mapcode\":\"SQB.NR3\",\"mapcodeInAlphabet\":\"ΣΘΒ.ΝΨ3\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"BEL\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"Ψ8ΨΝ.07Ζ\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"BEL\"},{\"mapcode\":\"0L46.LG9\",\"mapcodeInAlphabet\":\"0Λ46.ΛΓ9\",\"territory\":\"DEU\",\"territoryInAlphabet\":\"DEU\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"Ψ8ΨΝ.07Ζ\",\"territory\":\"FRA\",\"territoryInAlphabet\":\"ΕΨΑ\"},{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"ΦΠ0Λ6.9ΡΝΘ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"ΑΑΑ\"}]}",
            "{\"international\":{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"ЧП0Л6.9РЗФ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"ААА\"},\"mapcodes\":[{\"mapcode\":\"JL0.KP\",\"mapcodeInAlphabet\":\"ПЛ0.КР\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"ЛЭХ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"Я8ЯЗ.07Б\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"ЛЭХ\"},{\"mapcode\":\"SQB.NR3\",\"mapcodeInAlphabet\":\"ЦФВ.ЗЯ3\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"ВЕЛ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"Я8ЯЗ.07Б\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"ВЕЛ\"},{\"mapcode\":\"0L46.LG9\",\"mapcodeInAlphabet\":\"0Л46.ЛГ9\",\"territory\":\"DEU\",\"territoryInAlphabet\":\"ДЕЭ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"Я8ЯЗ.07Б\",\"territory\":\"FRA\",\"territoryInAlphabet\":\"ЖЯА\"},{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"ЧП0Л6.9РЗФ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"ААА\"}]}",
            "{\"international\":{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"צט0ך6.9םלמ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"אאא\"},\"mapcodes\":[{\"mapcode\":\"JL0.KP\",\"mapcodeInAlphabet\":\"טך0.ים\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"ךץר\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"נ8נל.07ת\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"ךץר\"},{\"mapcode\":\"SQB.NR3\",\"mapcodeInAlphabet\":\"עמב.לנ3\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"בףך\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"נ8נל.07ת\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"בףך\"},{\"mapcode\":\"0L46.LG9\",\"mapcodeInAlphabet\":\"0ך46.ךז9\",\"territory\":\"DEU\",\"territoryInAlphabet\":\"דףץ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"נ8נל.07ת\",\"territory\":\"FRA\",\"territoryInAlphabet\":\"הנא\"},{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"צט0ך6.9םלמ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"אאא\"}]}",
            "{\"international\":{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"लठ0त6.9नधप\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"अअअ\"},\"mapcodes\":[{\"mapcode\":\"JL0.KP\",\"mapcodeInAlphabet\":\"ठत0.णन\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"तफस\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"भ8भध.07ड\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"तफस\"},{\"mapcode\":\"SQB.NR3\",\"mapcodeInAlphabet\":\"मपक.धभ3\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"कएत\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"भ8भध.07ड\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"कएत\"},{\"mapcode\":\"0L46.LG9\",\"mapcodeInAlphabet\":\"0त46.तज9\",\"territory\":\"DEU\",\"territoryInAlphabet\":\"घएफ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"भ8भध.07ड\",\"territory\":\"FRA\",\"territoryInAlphabet\":\"चभअ\"},{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"लठ0त6.9नधप\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"अअअ\"}]}",
            "{\"international\":{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"ഴഡ0ഥ6.9നധമ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"ഒഒഒ\"},\"mapcodes\":[{\"mapcode\":\"JL0.KP\",\"mapcodeInAlphabet\":\"ഡഥ0.തന\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"ഥഉശ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"ര8രധ.07ഹ\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"ഥഉശ\"},{\"mapcode\":\"SQB.NR3\",\"mapcodeInAlphabet\":\"റമക.ധര3\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"കഋഥ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"ര8രധ.07ഹ\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"കഋഥ\"},{\"mapcode\":\"0L46.LG9\",\"mapcodeInAlphabet\":\"0ഥ46.ഥജ9\",\"territory\":\"DEU\",\"territoryInAlphabet\":\"ഗഋഉ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"ര8രധ.07ഹ\",\"territory\":\"FRA\",\"territoryInAlphabet\":\"ചരഒ\"},{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"ഴഡ0ഥ6.9നധമ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"ഒഒഒ\"}]}",
            "{\"international\":{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"ႺႮ0Ⴑ6.9ႵႴႶ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"ႠႠႠ\"},\"mapcodes\":[{\"mapcode\":\"JL0.KP\",\"mapcodeInAlphabet\":\"ႮႱ0.ႰႵ\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"ႱႨႽ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"Ⴗ8ႷႴ.07Ⴟ\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"ႱႨႽ\"},{\"mapcode\":\"SQB.NR3\",\"mapcodeInAlphabet\":\"ႸႶႡ.ႴႷ3\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"ႡႤႱ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"Ⴗ8ႷႴ.07Ⴟ\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"ႡႤႱ\"},{\"mapcode\":\"0L46.LG9\",\"mapcodeInAlphabet\":\"0Ⴑ46.ႱႫ9\",\"territory\":\"DEU\",\"territoryInAlphabet\":\"ႦႤႨ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"Ⴗ8ႷႴ.07Ⴟ\",\"territory\":\"FRA\",\"territoryInAlphabet\":\"ႩႷႠ\"},{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"ႺႮ0Ⴑ6.9ႵႴႶ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"ႠႠႠ\"}]}",
            "{\"international\":{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"モス0ト6.9ヒヌフ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"アアア\"},\"mapcodes\":[{\"mapcode\":\"JL0.KP\",\"mapcodeInAlphabet\":\"スト0.チヒ\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"トエラ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"ヘ8ヘヌ.07ヲ\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"トエラ\"},{\"mapcode\":\"SQB.NR3\",\"mapcodeInAlphabet\":\"ホフカ.ヌヘ3\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"カオト\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"ヘ8ヘヌ.07ヲ\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"カオト\"},{\"mapcode\":\"0L46.LG9\",\"mapcodeInAlphabet\":\"0ト46.トコ9\",\"territory\":\"DEU\",\"territoryInAlphabet\":\"クオエ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"ヘ8ヘヌ.07ヲ\",\"territory\":\"FRA\",\"territoryInAlphabet\":\"ケヘア\"},{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"モス0ト6.9ヒヌフ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"アアア\"}]}",
            "{\"international\":{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"ลช0ด6.9ธทบ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"ะะะ\"},\"mapcodes\":[{\"mapcode\":\"JL0.KP\",\"mapcodeInAlphabet\":\"ชด0.ฑธ\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"ดฬอ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"ผ8ผท.07ฯ\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"ดฬอ\"},{\"mapcode\":\"SQB.NR3\",\"mapcodeInAlphabet\":\"มบก.ทผ3\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"กาด\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"ผ8ผท.07ฯ\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"กาด\"},{\"mapcode\":\"0L46.LG9\",\"mapcodeInAlphabet\":\"0ด46.ดจ9\",\"territory\":\"DEU\",\"territoryInAlphabet\":\"คาฬ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"ผ8ผท.07ฯ\",\"territory\":\"FRA\",\"territoryInAlphabet\":\"งผะ\"},{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"ลช0ด6.9ธทบ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"ะะะ\"}]}",
            "{\"international\":{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"ວຍ0ທ6.9ຜບພ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"ະະະ\"},\"mapcodes\":[{\"mapcode\":\"JL0.KP\",\"mapcodeInAlphabet\":\"ຍທ0.ດຜ\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"ທຽຫ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"ມ8ມບ.07ຯ\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"ທຽຫ\"},{\"mapcode\":\"SQB.NR3\",\"mapcodeInAlphabet\":\"ຢພກ.ບມ3\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"ກໃທ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"ມ8ມບ.07ຯ\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"ກໃທ\"},{\"mapcode\":\"0L46.LG9\",\"mapcodeInAlphabet\":\"0ທ46.ທຈ9\",\"territory\":\"DEU\",\"territoryInAlphabet\":\"ຄໃຽ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"ມ8ມບ.07ຯ\",\"territory\":\"FRA\",\"territoryInAlphabet\":\"ງມະ\"},{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"ວຍ0ທ6.9ຜບພ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"ະະະ\"}]}",
            "{\"international\":{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"ՏԽ0Հ6.9ՇՃՈ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"ՖՖՖ\"},\"mapcodes\":[{\"mapcode\":\"JL0.KP\",\"mapcodeInAlphabet\":\"ԽՀ0.ԿՇ\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"ՀՅՑ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"Պ8ՊՃ.07Փ\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"ՀՅՑ\"},{\"mapcode\":\"SQB.NR3\",\"mapcodeInAlphabet\":\"ՍՈԲ.ՃՊ3\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"ԲԵՀ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"Պ8ՊՃ.07Փ\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"ԲԵՀ\"},{\"mapcode\":\"0L46.LG9\",\"mapcodeInAlphabet\":\"0Հ46.ՀԹ9\",\"territory\":\"DEU\",\"territoryInAlphabet\":\"ԴԵՅ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"Պ8ՊՃ.07Փ\",\"territory\":\"FRA\",\"territoryInAlphabet\":\"ԸՊՖ\"},{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"ՏԽ0Հ6.9ՇՃՈ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"ՖՖՖ\"}]}",
            "{\"international\":{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"বঝ0ড6.9তণথ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"অঅঅ\"},\"mapcodes\":[{\"mapcode\":\"JL0.KP\",\"mapcodeInAlphabet\":\"ঝড0.ঠত\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"ডওয\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"দ8দণ.07হ\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"ডওয\"},{\"mapcode\":\"SQB.NR3\",\"mapcodeInAlphabet\":\"নথঌ.ণদ3\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"ঌএড\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"দ8দণ.07হ\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"ঌএড\"},{\"mapcode\":\"0L46.LG9\",\"mapcodeInAlphabet\":\"0ড46.ডঙ9\",\"territory\":\"DEU\",\"territoryInAlphabet\":\"খএও\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"দ8দণ.07হ\",\"territory\":\"FRA\",\"territoryInAlphabet\":\"গদঅ\"},{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"বঝ0ড6.9তণথ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"অঅঅ\"}]}",
            "{\"international\":{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"ਲਠ0ਤ6.9ਨਧਪ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"ਅਅਅ\"},\"mapcodes\":[{\"mapcode\":\"JL0.KP\",\"mapcodeInAlphabet\":\"ਠਤ0.ਣਨ\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"ਤਫਸ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"ਭ8ਭਧ.07ਡ\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"ਤਫਸ\"},{\"mapcode\":\"SQB.NR3\",\"mapcodeInAlphabet\":\"ਮਪਕ.ਧਭ3\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"ਕਏਤ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"ਭ8ਭਧ.07ਡ\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"ਕਏਤ\"},{\"mapcode\":\"0L46.LG9\",\"mapcodeInAlphabet\":\"0ਤ46.ਤਜ9\",\"territory\":\"DEU\",\"territoryInAlphabet\":\"ਘਏਫ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"ਭ8ਭਧ.07ਡ\",\"territory\":\"FRA\",\"territoryInAlphabet\":\"ਚਭਅ\"},{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"ਲਠ0ਤ6.9ਨਧਪ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"ਅਅਅ\"}]}",
            "{\"international\":{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"ཟཇ0ཌ6.9དཏན\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"མམམ\"},\"mapcodes\":[{\"mapcode\":\"JL0.KP\",\"mapcodeInAlphabet\":\"ཇཌ0.ཊད\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"ཌཥར\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"པ8པཏ.07ས\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"ཌཥར\"},{\"mapcode\":\"SQB.NR3\",\"mapcodeInAlphabet\":\"བནཀ.ཏཔ3\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"ཀཤཌ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"པ8པཏ.07ས\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"ཀཤཌ\"},{\"mapcode\":\"0L46.LG9\",\"mapcodeInAlphabet\":\"0ཌ46.ཌཅ9\",\"territory\":\"DEU\",\"territoryInAlphabet\":\"གཤཥ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"པ8པཏ.07ས\",\"territory\":\"FRA\",\"territoryInAlphabet\":\"ངཔམ\"},{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"ཟཇ0ཌ6.9དཏན\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"མམམ\"}]}",
    };
    private final static String[] EXPECTED_ALPHABETS_XML = {
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcodes><international><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>VJ0L6.9PNQ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>AAA</territoryInAlphabet></international><mapcodes><mapcode><mapcode>JL0.KP</mapcode><mapcodeInAlphabet>JL0.KP</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>LUX</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>R8RN.07Z</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>LUX</territoryInAlphabet></mapcode><mapcode><mapcode>SQB.NR3</mapcode><mapcodeInAlphabet>SQB.NR3</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>BEL</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>R8RN.07Z</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>BEL</territoryInAlphabet></mapcode><mapcode><mapcode>0L46.LG9</mapcode><mapcodeInAlphabet>0L46.LG9</mapcodeInAlphabet><territory>DEU</territory><territoryInAlphabet>DEU</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>R8RN.07Z</mapcodeInAlphabet><territory>FRA</territory><territoryInAlphabet>FRA</territoryInAlphabet></mapcode><mapcode><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>VJ0L6.9PNQ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>AAA</territoryInAlphabet></mapcode></mapcodes></mapcodes>",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcodes><international><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>ΦΠ0Λ6.9ΡΝΘ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>ΑΑΑ</territoryInAlphabet></international><mapcodes><mapcode><mapcode>JL0.KP</mapcode><mapcodeInAlphabet>ΠΛ0.ΚΡ</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>LUX</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>Ψ8ΨΝ.07Ζ</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>LUX</territoryInAlphabet></mapcode><mapcode><mapcode>SQB.NR3</mapcode><mapcodeInAlphabet>ΣΘΒ.ΝΨ3</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>BEL</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>Ψ8ΨΝ.07Ζ</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>BEL</territoryInAlphabet></mapcode><mapcode><mapcode>0L46.LG9</mapcode><mapcodeInAlphabet>0Λ46.ΛΓ9</mapcodeInAlphabet><territory>DEU</territory><territoryInAlphabet>DEU</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>Ψ8ΨΝ.07Ζ</mapcodeInAlphabet><territory>FRA</territory><territoryInAlphabet>ΕΨΑ</territoryInAlphabet></mapcode><mapcode><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>ΦΠ0Λ6.9ΡΝΘ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>ΑΑΑ</territoryInAlphabet></mapcode></mapcodes></mapcodes>",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcodes><international><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>ЧП0Л6.9РЗФ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>ААА</territoryInAlphabet></international><mapcodes><mapcode><mapcode>JL0.KP</mapcode><mapcodeInAlphabet>ПЛ0.КР</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>ЛЭХ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>Я8ЯЗ.07Б</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>ЛЭХ</territoryInAlphabet></mapcode><mapcode><mapcode>SQB.NR3</mapcode><mapcodeInAlphabet>ЦФВ.ЗЯ3</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>ВЕЛ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>Я8ЯЗ.07Б</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>ВЕЛ</territoryInAlphabet></mapcode><mapcode><mapcode>0L46.LG9</mapcode><mapcodeInAlphabet>0Л46.ЛГ9</mapcodeInAlphabet><territory>DEU</territory><territoryInAlphabet>ДЕЭ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>Я8ЯЗ.07Б</mapcodeInAlphabet><territory>FRA</territory><territoryInAlphabet>ЖЯА</territoryInAlphabet></mapcode><mapcode><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>ЧП0Л6.9РЗФ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>ААА</territoryInAlphabet></mapcode></mapcodes></mapcodes>",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcodes><international><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>צט0ך6.9םלמ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>אאא</territoryInAlphabet></international><mapcodes><mapcode><mapcode>JL0.KP</mapcode><mapcodeInAlphabet>טך0.ים</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>ךץר</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>נ8נל.07ת</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>ךץר</territoryInAlphabet></mapcode><mapcode><mapcode>SQB.NR3</mapcode><mapcodeInAlphabet>עמב.לנ3</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>בףך</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>נ8נל.07ת</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>בףך</territoryInAlphabet></mapcode><mapcode><mapcode>0L46.LG9</mapcode><mapcodeInAlphabet>0ך46.ךז9</mapcodeInAlphabet><territory>DEU</territory><territoryInAlphabet>דףץ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>נ8נל.07ת</mapcodeInAlphabet><territory>FRA</territory><territoryInAlphabet>הנא</territoryInAlphabet></mapcode><mapcode><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>צט0ך6.9םלמ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>אאא</territoryInAlphabet></mapcode></mapcodes></mapcodes>",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcodes><international><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>लठ0त6.9नधप</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>अअअ</territoryInAlphabet></international><mapcodes><mapcode><mapcode>JL0.KP</mapcode><mapcodeInAlphabet>ठत0.णन</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>तफस</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>भ8भध.07ड</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>तफस</territoryInAlphabet></mapcode><mapcode><mapcode>SQB.NR3</mapcode><mapcodeInAlphabet>मपक.धभ3</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>कएत</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>भ8भध.07ड</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>कएत</territoryInAlphabet></mapcode><mapcode><mapcode>0L46.LG9</mapcode><mapcodeInAlphabet>0त46.तज9</mapcodeInAlphabet><territory>DEU</territory><territoryInAlphabet>घएफ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>भ8भध.07ड</mapcodeInAlphabet><territory>FRA</territory><territoryInAlphabet>चभअ</territoryInAlphabet></mapcode><mapcode><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>लठ0त6.9नधप</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>अअअ</territoryInAlphabet></mapcode></mapcodes></mapcodes>",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcodes><international><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>ഴഡ0ഥ6.9നധമ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>ഒഒഒ</territoryInAlphabet></international><mapcodes><mapcode><mapcode>JL0.KP</mapcode><mapcodeInAlphabet>ഡഥ0.തന</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>ഥഉശ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>ര8രധ.07ഹ</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>ഥഉശ</territoryInAlphabet></mapcode><mapcode><mapcode>SQB.NR3</mapcode><mapcodeInAlphabet>റമക.ധര3</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>കഋഥ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>ര8രധ.07ഹ</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>കഋഥ</territoryInAlphabet></mapcode><mapcode><mapcode>0L46.LG9</mapcode><mapcodeInAlphabet>0ഥ46.ഥജ9</mapcodeInAlphabet><territory>DEU</territory><territoryInAlphabet>ഗഋഉ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>ര8രധ.07ഹ</mapcodeInAlphabet><territory>FRA</territory><territoryInAlphabet>ചരഒ</territoryInAlphabet></mapcode><mapcode><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>ഴഡ0ഥ6.9നധമ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>ഒഒഒ</territoryInAlphabet></mapcode></mapcodes></mapcodes>",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcodes><international><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>ႺႮ0Ⴑ6.9ႵႴႶ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>ႠႠႠ</territoryInAlphabet></international><mapcodes><mapcode><mapcode>JL0.KP</mapcode><mapcodeInAlphabet>ႮႱ0.ႰႵ</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>ႱႨႽ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>Ⴗ8ႷႴ.07Ⴟ</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>ႱႨႽ</territoryInAlphabet></mapcode><mapcode><mapcode>SQB.NR3</mapcode><mapcodeInAlphabet>ႸႶႡ.ႴႷ3</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>ႡႤႱ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>Ⴗ8ႷႴ.07Ⴟ</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>ႡႤႱ</territoryInAlphabet></mapcode><mapcode><mapcode>0L46.LG9</mapcode><mapcodeInAlphabet>0Ⴑ46.ႱႫ9</mapcodeInAlphabet><territory>DEU</territory><territoryInAlphabet>ႦႤႨ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>Ⴗ8ႷႴ.07Ⴟ</mapcodeInAlphabet><territory>FRA</territory><territoryInAlphabet>ႩႷႠ</territoryInAlphabet></mapcode><mapcode><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>ႺႮ0Ⴑ6.9ႵႴႶ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>ႠႠႠ</territoryInAlphabet></mapcode></mapcodes></mapcodes>",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcodes><international><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>モス0ト6.9ヒヌフ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>アアア</territoryInAlphabet></international><mapcodes><mapcode><mapcode>JL0.KP</mapcode><mapcodeInAlphabet>スト0.チヒ</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>トエラ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>ヘ8ヘヌ.07ヲ</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>トエラ</territoryInAlphabet></mapcode><mapcode><mapcode>SQB.NR3</mapcode><mapcodeInAlphabet>ホフカ.ヌヘ3</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>カオト</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>ヘ8ヘヌ.07ヲ</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>カオト</territoryInAlphabet></mapcode><mapcode><mapcode>0L46.LG9</mapcode><mapcodeInAlphabet>0ト46.トコ9</mapcodeInAlphabet><territory>DEU</territory><territoryInAlphabet>クオエ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>ヘ8ヘヌ.07ヲ</mapcodeInAlphabet><territory>FRA</territory><territoryInAlphabet>ケヘア</territoryInAlphabet></mapcode><mapcode><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>モス0ト6.9ヒヌフ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>アアア</territoryInAlphabet></mapcode></mapcodes></mapcodes>",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcodes><international><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>ลช0ด6.9ธทบ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>ะะะ</territoryInAlphabet></international><mapcodes><mapcode><mapcode>JL0.KP</mapcode><mapcodeInAlphabet>ชด0.ฑธ</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>ดฬอ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>ผ8ผท.07ฯ</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>ดฬอ</territoryInAlphabet></mapcode><mapcode><mapcode>SQB.NR3</mapcode><mapcodeInAlphabet>มบก.ทผ3</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>กาด</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>ผ8ผท.07ฯ</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>กาด</territoryInAlphabet></mapcode><mapcode><mapcode>0L46.LG9</mapcode><mapcodeInAlphabet>0ด46.ดจ9</mapcodeInAlphabet><territory>DEU</territory><territoryInAlphabet>คาฬ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>ผ8ผท.07ฯ</mapcodeInAlphabet><territory>FRA</territory><territoryInAlphabet>งผะ</territoryInAlphabet></mapcode><mapcode><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>ลช0ด6.9ธทบ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>ะะะ</territoryInAlphabet></mapcode></mapcodes></mapcodes>",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcodes><international><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>ວຍ0ທ6.9ຜບພ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>ະະະ</territoryInAlphabet></international><mapcodes><mapcode><mapcode>JL0.KP</mapcode><mapcodeInAlphabet>ຍທ0.ດຜ</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>ທຽຫ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>ມ8ມບ.07ຯ</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>ທຽຫ</territoryInAlphabet></mapcode><mapcode><mapcode>SQB.NR3</mapcode><mapcodeInAlphabet>ຢພກ.ບມ3</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>ກໃທ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>ມ8ມບ.07ຯ</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>ກໃທ</territoryInAlphabet></mapcode><mapcode><mapcode>0L46.LG9</mapcode><mapcodeInAlphabet>0ທ46.ທຈ9</mapcodeInAlphabet><territory>DEU</territory><territoryInAlphabet>ຄໃຽ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>ມ8ມບ.07ຯ</mapcodeInAlphabet><territory>FRA</territory><territoryInAlphabet>ງມະ</territoryInAlphabet></mapcode><mapcode><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>ວຍ0ທ6.9ຜບພ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>ະະະ</territoryInAlphabet></mapcode></mapcodes></mapcodes>",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcodes><international><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>ՏԽ0Հ6.9ՇՃՈ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>ՖՖՖ</territoryInAlphabet></international><mapcodes><mapcode><mapcode>JL0.KP</mapcode><mapcodeInAlphabet>ԽՀ0.ԿՇ</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>ՀՅՑ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>Պ8ՊՃ.07Փ</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>ՀՅՑ</territoryInAlphabet></mapcode><mapcode><mapcode>SQB.NR3</mapcode><mapcodeInAlphabet>ՍՈԲ.ՃՊ3</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>ԲԵՀ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>Պ8ՊՃ.07Փ</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>ԲԵՀ</territoryInAlphabet></mapcode><mapcode><mapcode>0L46.LG9</mapcode><mapcodeInAlphabet>0Հ46.ՀԹ9</mapcodeInAlphabet><territory>DEU</territory><territoryInAlphabet>ԴԵՅ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>Պ8ՊՃ.07Փ</mapcodeInAlphabet><territory>FRA</territory><territoryInAlphabet>ԸՊՖ</territoryInAlphabet></mapcode><mapcode><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>ՏԽ0Հ6.9ՇՃՈ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>ՖՖՖ</territoryInAlphabet></mapcode></mapcodes></mapcodes>",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcodes><international><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>বঝ0ড6.9তণথ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>অঅঅ</territoryInAlphabet></international><mapcodes><mapcode><mapcode>JL0.KP</mapcode><mapcodeInAlphabet>ঝড0.ঠত</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>ডওয</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>দ8দণ.07হ</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>ডওয</territoryInAlphabet></mapcode><mapcode><mapcode>SQB.NR3</mapcode><mapcodeInAlphabet>নথঌ.ণদ3</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>ঌএড</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>দ8দণ.07হ</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>ঌএড</territoryInAlphabet></mapcode><mapcode><mapcode>0L46.LG9</mapcode><mapcodeInAlphabet>0ড46.ডঙ9</mapcodeInAlphabet><territory>DEU</territory><territoryInAlphabet>খএও</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>দ8দণ.07হ</mapcodeInAlphabet><territory>FRA</territory><territoryInAlphabet>গদঅ</territoryInAlphabet></mapcode><mapcode><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>বঝ0ড6.9তণথ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>অঅঅ</territoryInAlphabet></mapcode></mapcodes></mapcodes>",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcodes><international><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>ਲਠ0ਤ6.9ਨਧਪ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>ਅਅਅ</territoryInAlphabet></international><mapcodes><mapcode><mapcode>JL0.KP</mapcode><mapcodeInAlphabet>ਠਤ0.ਣਨ</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>ਤਫਸ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>ਭ8ਭਧ.07ਡ</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>ਤਫਸ</territoryInAlphabet></mapcode><mapcode><mapcode>SQB.NR3</mapcode><mapcodeInAlphabet>ਮਪਕ.ਧਭ3</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>ਕਏਤ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>ਭ8ਭਧ.07ਡ</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>ਕਏਤ</territoryInAlphabet></mapcode><mapcode><mapcode>0L46.LG9</mapcode><mapcodeInAlphabet>0ਤ46.ਤਜ9</mapcodeInAlphabet><territory>DEU</territory><territoryInAlphabet>ਘਏਫ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>ਭ8ਭਧ.07ਡ</mapcodeInAlphabet><territory>FRA</territory><territoryInAlphabet>ਚਭਅ</territoryInAlphabet></mapcode><mapcode><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>ਲਠ0ਤ6.9ਨਧਪ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>ਅਅਅ</territoryInAlphabet></mapcode></mapcodes></mapcodes>",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcodes><international><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>ཟཇ0ཌ6.9དཏན</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>མམམ</territoryInAlphabet></international><mapcodes><mapcode><mapcode>JL0.KP</mapcode><mapcodeInAlphabet>ཇཌ0.ཊད</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>ཌཥར</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>པ8པཏ.07ས</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>ཌཥར</territoryInAlphabet></mapcode><mapcode><mapcode>SQB.NR3</mapcode><mapcodeInAlphabet>བནཀ.ཏཔ3</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>ཀཤཌ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>པ8པཏ.07ས</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>ཀཤཌ</territoryInAlphabet></mapcode><mapcode><mapcode>0L46.LG9</mapcode><mapcodeInAlphabet>0ཌ46.ཌཅ9</mapcodeInAlphabet><territory>DEU</territory><territoryInAlphabet>གཤཥ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>པ8པཏ.07ས</mapcodeInAlphabet><territory>FRA</territory><territoryInAlphabet>ངཔམ</territoryInAlphabet></mapcode><mapcode><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>ཟཇ0ཌ6.9དཏན</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>མམམ</territoryInAlphabet></mapcode></mapcodes></mapcodes>"
    };
}
