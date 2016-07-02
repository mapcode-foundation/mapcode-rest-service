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
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.ExecutionContext;

import javax.annotation.Nonnull;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class RootResourceTest {
    private static final Logger LOG = LoggerFactory.getLogger(RootResourceTest.class);

    private static final String TEST_POM_VERSION = "TEST-1";

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
    private static TJWSEmbeddedJaxrsServer server;

    @BeforeClass
    public static void startServer() {
        server = new TJWSEmbeddedJaxrsServer();
        server.setPort(PORT);

        // Create a simple ResourceProcessor, required for implementation of REST service.
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
                return null;
            }
        };
        final ResourceProcessor resourceProcessor = new ResourceProcessor(reactor);

        // Add root.
        server.getDeployment().getResources().add(new RootResourceImpl(
                new MavenProperties(TEST_POM_VERSION),
                new SystemMetricsImpl()
        ));

        server.getDeployment().getResources().add(new MapcodeResourceImpl(
                resourceProcessor,
                new SystemMetricsImpl()
        ));
        server.start();
    }

    @AfterClass
    public static void stopServer() {
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
        Assert.assertEquals("{\"version\":\"TEST-1\"}",
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
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><version><version>TEST-1</version></version>",
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

    @Test
    public void checkCodesIncludeGreekJson() {
        LOG.info("checkCodesIncludeGreekJson");
        final Response response = new ResteasyClientBuilder().build().
                target(HOST + "/mapcode/codes/" + TEST_LATLON2 + "?include=offset,territory,alphabet&alphabet=greek").
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"local\":{\"mapcode\":\"QKM.N4\",\"mapcodeInAlphabet\":\"ΘΚΜ.Ν4\",\"territory\":\"NLD\",\"territoryInAlphabet\":\"ΝΛΔ\",\"offsetMeters\":2.843693},\"international\":{\"mapcode\":\"VHVN4.YZ74\",\"mapcodeInAlphabet\":\"ΦΗΦΝ4.ΥΖ74\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"ΑΑΑ\",\"offsetMeters\":1.907245},\"mapcodes\":[{\"mapcode\":\"QKM.N4\",\"mapcodeInAlphabet\":\"ΘΚΜ.Ν4\",\"territory\":\"NLD\",\"territoryInAlphabet\":\"ΝΛΔ\",\"offsetMeters\":2.843693},{\"mapcode\":\"CZQ.376\",\"mapcodeInAlphabet\":\"ΞΖΘ.376\",\"territory\":\"NLD\",\"territoryInAlphabet\":\"ΝΛΔ\",\"offsetMeters\":5.004936},{\"mapcode\":\"N39J.QW0\",\"mapcodeInAlphabet\":\"Ν39Π.ΘΩ0\",\"territory\":\"NLD\",\"territoryInAlphabet\":\"ΝΛΔ\",\"offsetMeters\":2.836538},{\"mapcode\":\"VHVN4.YZ74\",\"mapcodeInAlphabet\":\"ΦΗΦΝ4.ΥΖ74\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"ΑΑΑ\",\"offsetMeters\":1.907245}]}",
                response.readEntity(String.class));
    }

    @Test
    public void checkCodesIncludeGreekXml() {
        LOG.info("checkCodesIncludeGreekXml");
        final Response response = new ResteasyClientBuilder().build().
                target(HOST + "/mapcode/codes/" + TEST_LATLON2 + "?include=offset,territory,alphabet&alphabet=greek").
                request().
                accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcodes><local><mapcode>QKM.N4</mapcode><mapcodeInAlphabet>ΘΚΜ.Ν4</mapcodeInAlphabet><territory>NLD</territory><territoryInAlphabet>ΝΛΔ</territoryInAlphabet><offsetMeters>2.843693</offsetMeters></local><international><mapcode>VHVN4.YZ74</mapcode><mapcodeInAlphabet>ΦΗΦΝ4.ΥΖ74</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>ΑΑΑ</territoryInAlphabet><offsetMeters>1.907245</offsetMeters></international><mapcodes><mapcode><mapcode>QKM.N4</mapcode><mapcodeInAlphabet>ΘΚΜ.Ν4</mapcodeInAlphabet><territory>NLD</territory><territoryInAlphabet>ΝΛΔ</territoryInAlphabet><offsetMeters>2.843693</offsetMeters></mapcode><mapcode><mapcode>CZQ.376</mapcode><mapcodeInAlphabet>ΞΖΘ.376</mapcodeInAlphabet><territory>NLD</territory><territoryInAlphabet>ΝΛΔ</territoryInAlphabet><offsetMeters>5.004936</offsetMeters></mapcode><mapcode><mapcode>N39J.QW0</mapcode><mapcodeInAlphabet>Ν39Π.ΘΩ0</mapcodeInAlphabet><territory>NLD</territory><territoryInAlphabet>ΝΛΔ</territoryInAlphabet><offsetMeters>2.836538</offsetMeters></mapcode><mapcode><mapcode>VHVN4.YZ74</mapcode><mapcodeInAlphabet>ΦΗΦΝ4.ΥΖ74</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>ΑΑΑ</territoryInAlphabet><offsetMeters>1.907245</offsetMeters></mapcode></mapcodes></mapcodes>",
                response.readEntity(String.class));
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
        LOG.info("response=\n{}", r);
        Assert.assertTrue(r.startsWith("{\"territories\":[{\"alphaCode\":\"USA\",\"alphaCodeMinimalUnambiguous\":\"USA\",\"alphaCodeMinimal\":\"USA\",\"fullName\":\"USA\",\"aliases\":[\"US\"],\"fullNameAliases\":[\"United States of America\",\"America\"]},{\"alphaCode\":\"IND\",\"alphaCodeMinimalUnambiguous\":\"IND\",\"alphaCodeMinimal\":\"IND\",\"fullName\":\"India\",\"aliases\":[\"IN\"]},{\"alphaCode\":\"CAN\",\"alphaCodeMinimalUnambiguous\":\"CAN\",\"alphaCodeMinimal\":\"CAN\",\"fullName\":\"Canada\",\"aliases\":[\"CA\"]},{\"alphaCode\":\"AUS\",\"alphaCodeMinimalUnambiguous\":\"AUS\",\"alphaCodeMinimal\":\"AUS\",\"fullName\":\"Australia\",\"aliases\":[\"AU\"]},{\"alphaCode\":\"MEX\",\"alphaCodeMinimalUnambiguous\":\"MEX\",\"alphaCodeMinimal\":\"MEX\",\"fullName\":\"Mexico\",\"aliases\":[\"MX\"]},{\"alphaCode\":\"BRA\",\"alphaCodeMinimalUnambiguous\":\"BRA\",\"alphaCodeMinimal\":\"BRA\",\"fullName\":\"Brazil\",\"aliases\":[\"BR\"]},{\"alphaCode\":\"RUS\",\"alphaCodeMinimalUnambiguous\":\"RUS\",\"alphaCodeMinimal\":\"RUS\",\"fullName\":\"Russia\",\"aliases\":[\"RU\"]},{\"alphaCode\":\"CHN\",\"alphaCodeMinimalUnambiguous\":\"CHN\",\"alphaCodeMinimal\":\"CHN\",\"fullName\":\"China\",\"aliases\":[\"CN\"]},{\"alphaCode\":\"ATA\",\"alphaCodeMinimalUnambiguous\":\"ATA\",\"alphaCodeMinimal\":\"ATA\",\"fullName\":\"Antarctica\"},{\"alphaCode\":\"VAT\",\"alphaCodeMinimalUnambiguous\":\"VAT\",\"alphaCodeMinimal\":\"VAT\",\"fullName\":\"Vatican City\",\"fullNameAliases\":[\"Holy See)\"]},{\"alphaCode\":\"MCO\",\"alphaCodeMinimalUnambiguous\":\"MCO\",\"alphaCodeMinimal\":\"MCO\",\"fullName\":\"Monaco\"},{\"alphaCode\":\"GIB\",\"alphaCodeMinimalUnambiguous\":\"GIB\",\"alphaCodeMinimal\":\"GIB\",\"fullName\":\"Gibraltar\"},{\"alphaCode\":\"TKL\",\"alphaCodeMinimalUnambiguous\":\"TKL\",\"alphaCodeMinimal\":\"TKL\",\"fullName\":\"Tokelau\"},{\"alphaCode\":\"CCK\",\"alphaCodeMinimalUnambiguous\":\"CCK\",\"alphaCodeMinimal\":\"CCK\",\"fullName\":\"Cocos Islands\",\"aliases\":[\"AU-CC\",\"AUS-CC\"],\"fullNameAliases\":[\"Keeling Islands\"]}"));
        Assert.assertTrue(r.endsWith("\"alphaCodeMinimal\":\"XZ\",\"fullName\":\"Xizang\",\"parentTerritory\":\"CHN\",\"aliases\":[\"CN-54\"],\"fullNameAliases\":[\"Tibet\"]},{\"alphaCode\":\"CN-GS\",\"alphaCodeMinimalUnambiguous\":\"GS\",\"alphaCodeMinimal\":\"GS\",\"fullName\":\"Gansu\",\"parentTerritory\":\"CHN\",\"aliases\":[\"CN-62\"]},{\"alphaCode\":\"CN-QH\",\"alphaCodeMinimalUnambiguous\":\"QH\",\"alphaCodeMinimal\":\"QH\",\"fullName\":\"Qinghai\",\"parentTerritory\":\"CHN\",\"aliases\":[\"CN-63\"]},{\"alphaCode\":\"CN-XJ\",\"alphaCodeMinimalUnambiguous\":\"XJ\",\"alphaCodeMinimal\":\"XJ\",\"fullName\":\"Xinjiang Uyghur\",\"parentTerritory\":\"CHN\",\"aliases\":[\"CN-65\"]},{\"alphaCode\":\"UMI\",\"alphaCodeMinimalUnambiguous\":\"UMI\",\"alphaCodeMinimal\":\"UMI\",\"fullName\":\"United States Minor Outlying Islands\",\"aliases\":[\"US-UM\",\"USA-UM\",\"JTN\"]},{\"alphaCode\":\"CPT\",\"alphaCodeMinimalUnambiguous\":\"CPT\",\"alphaCodeMinimal\":"));
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
        LOG.info("response=\n{}", r);
        Assert.assertTrue(r.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><territories><territory><alphaCode>USA</alphaCode><alphaCodeMinimalUnambiguous>USA</alphaCodeMinimalUnambiguous><alphaCodeMinimal>USA</alphaCodeMinimal><fullName>USA</fullName><aliases><alias>US</alias></aliases><fullNameAliases><fullNameAlias>United States of America</fullNameAlias><fullNameAlias>America</fullNameAlias></fullNameAliases></territory><territory><alphaCode>IND</alphaCode><alphaCodeMinimalUnambiguous>IND</alphaCodeMinimalUnambiguous><alphaCodeMinimal>IND</alphaCodeMinimal><fullName>India</fullName><aliases><alias>IN</alias></aliases><fullNameAliases/></territory><territory><alphaCode>CAN</alphaCode><alphaCodeMinimalUnambiguous>CAN</alphaCodeMinimalUnambiguous><alphaCodeMinimal>CAN</alphaCodeMinimal><fullName>Canada</fullName><aliases><alias>CA</alias></aliases><fullNameAliases/></territory><territory><alphaCode>AUS</alphaCode><alphaCodeMinimalUnambiguous>AUS</alphaCodeMinimalUnambiguous><alphaCodeMinimal>AUS</alphaCodeMinimal><fullName>Australia</fullName><aliases><alias>AU</alias></aliases><fullNameAliases/></territory><territory><alphaCode>MEX</alphaCode><alphaCodeMinimalUnambiguous>MEX</alphaCodeMinimalUnambiguous><alphaCodeMinimal>MEX</alphaCodeMinimal><fullName>Mexico</fullName><aliases><alias>MX</alias></aliases><fullNameAliases/></territory><territory><alphaCode>BRA</alphaCode><alphaCodeMinimalUnambiguous>BRA</alphaCodeMinimalUnambiguous><alphaCodeMinimal>BRA</alphaCodeMinimal>"));
        Assert.assertTrue(r.endsWith("<alphaCodeMinimal>UMI</alphaCodeMinimal><fullName>United States Minor Outlying Islands</fullName><aliases><alias>US-UM</alias><alias>USA-UM</alias><alias>JTN</alias></aliases><fullNameAliases/></territory><territory><alphaCode>CPT</alphaCode><alphaCodeMinimalUnambiguous>CPT</alphaCodeMinimalUnambiguous><alphaCodeMinimal>CPT</alphaCodeMinimal><fullName>Clipperton Island</fullName><aliases/><fullNameAliases/></territory><territory><alphaCode>AAA</alphaCode><alphaCodeMinimalUnambiguous>AAA</alphaCodeMinimalUnambiguous><alphaCodeMinimal>AAA</alphaCodeMinimal><fullName>International</fullName><aliases/><fullNameAliases><fullNameAlias>Worldwide</fullNameAlias><fullNameAlias>Earth</fullNameAlias></fullNameAliases></territory></territories>"));
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
        Assert.assertEquals("{\"alphabets\":[{\"name\":\"ROMAN\"},{\"name\":\"GREEK\"},{\"name\":\"CYRILLIC\"},{\"name\":\"HEBREW\"},{\"name\":\"HINDI\"},{\"name\":\"MALAY\"},{\"name\":\"GEORGIAN\"},{\"name\":\"KATAKANA\"},{\"name\":\"THAI\"},{\"name\":\"LAO\"},{\"name\":\"ARMENIAN\"},{\"name\":\"BENGALI\"},{\"name\":\"GURMUKHI\"},{\"name\":\"TIBETAN\"}]}",
                response.readEntity(String.class));
    }

    @Test
    public void checkAlphabetsXml() {
        LOG.info("checkAlphabetsXml");
        final Response response = new ResteasyClientBuilder().build().
                target(HOST + "/mapcode/alphabets").
                request().
                accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><alphabets><alphabet><name>ROMAN</name></alphabet><alphabet><name>GREEK</name></alphabet><alphabet><name>CYRILLIC</name></alphabet><alphabet><name>HEBREW</name></alphabet><alphabet><name>HINDI</name></alphabet><alphabet><name>MALAY</name></alphabet><alphabet><name>GEORGIAN</name></alphabet><alphabet><name>KATAKANA</name></alphabet><alphabet><name>THAI</name></alphabet><alphabet><name>LAO</name></alphabet><alphabet><name>ARMENIAN</name></alphabet><alphabet><name>BENGALI</name></alphabet><alphabet><name>GURMUKHI</name></alphabet><alphabet><name>TIBETAN</name></alphabet></alphabets>",
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
}
