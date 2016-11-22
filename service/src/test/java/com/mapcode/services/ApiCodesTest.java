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

import com.google.gson.Gson;
import com.mapcode.services.dto.MapcodeDTO;
import com.mapcode.services.dto.MapcodesDTO;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@SuppressWarnings("JUnitTestMethodWithNoAssertions")
public class ApiCodesTest {
    private static final Logger LOG = LoggerFactory.getLogger(ApiCodesTest.class);

    private static final Double TEST_LAT1 = 50.141706;
    private static final Double TEST_LON1 = 6.135864;
    private static final String TEST_LATLON1 = TEST_LAT1 + "," + TEST_LON1;

    private static final Double TEST_LAT2 = 52.159853;
    private static final Double TEST_LON2 = 4.499790;
    private static final String TEST_LATLON2 = TEST_LAT2 + "," + TEST_LON2;

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
    public void checkCodesNoLatLon() {
        LOG.info("checkCodesNoLatLon");
        final Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/codes")).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(403, response.getStatus());
    }

    @Test
    public void checkCodesUseOfContext() {
        LOG.info("checkCodesUseOfContext");
        final Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/codes/52,5?context=NLD")).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(400, response.getStatus());
    }

    @Test
    public void checkCoordsUseOfTerritory() {
        LOG.info("checkCoordsUseOfTerritory");
        final Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/coords/XX.XX?territory=NLD")).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(400, response.getStatus());
    }

    @Test
    public void checkCodesCheckLatLon() {
        LOG.info("checkCodesCheckLatLon");
        Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/codes/90,180")).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());

        response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/codes/-90,-180")).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());

        response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/codes/-91,0")).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(400, response.getStatus());

        response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/codes/91,0")).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(400, response.getStatus());

        response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/codes/0,-181")).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());

        response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/codes/0,181")).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
    }

    @Test
    public void checkCodesJson() {
        LOG.info("checkCodesJson");
        Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/codes/" + TEST_LATLON1)).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        String s = response.readEntity(String.class);
        Assert.assertEquals("{\"local\":{\"mapcode\":\"JL0.KP\",\"territory\":\"LUX\"},\"international\":{\"mapcode\":\"VJ0L6.9PNQ\"},\"mapcodes\":[{\"mapcode\":\"JL0.KP\",\"territory\":\"LUX\"},{\"mapcode\":\"R8RN.07Z\",\"territory\":\"LUX\"},{\"mapcode\":\"SQB.NR3\",\"territory\":\"BEL\"},{\"mapcode\":\"R8RN.07Z\",\"territory\":\"BEL\"},{\"mapcode\":\"0L46.LG9\",\"territory\":\"DEU\"},{\"mapcode\":\"R8RN.07Z\",\"territory\":\"FRA\"},{\"mapcode\":\"VJ0L6.9PNQ\"}]}",
                s);
        final MapcodesDTO x = new Gson().fromJson(s, MapcodesDTO.class);
        Assert.assertNotNull(x);
        Assert.assertEquals("VJ0L6.9PNQ", x.getInternational().getMapcode());
        Assert.assertNotNull(x.getLocal());
        Assert.assertEquals("JL0.KP", x.getLocal().getMapcode());
        final List<MapcodeDTO> mapcodes = x.getMapcodes();
        Assert.assertEquals(7, mapcodes.size());
        Assert.assertEquals("JL0.KP", mapcodes.get(0).getMapcode());
        Assert.assertEquals("LUX", mapcodes.get(0).getTerritory());

        response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/codes/" + TEST_LATLON1 + "?territory=LUX")).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        s = response.readEntity(String.class);
        Assert.assertEquals("{\"local\":{\"mapcode\":\"JL0.KP\",\"territory\":\"LUX\"},\"international\":{\"mapcode\":\"VJ0L6.9PNQ\"},\"mapcodes\":[{\"mapcode\":\"JL0.KP\",\"territory\":\"LUX\"},{\"mapcode\":\"R8RN.07Z\",\"territory\":\"LUX\"}]}",
                s);
        final MapcodesDTO y = new Gson().fromJson(s, MapcodesDTO.class);
        Assert.assertNotNull(y);
        Assert.assertEquals("JL0.KP", y.getLocal().getMapcode());
    }

    @Test
    public void checkCodesXml() {
        LOG.info("checkCodesXml");
        final Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/codes/" + TEST_LATLON1)).
                request().
                accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcodes><local><mapcode>JL0.KP</mapcode><territory>LUX</territory></local><international><mapcode>VJ0L6.9PNQ</mapcode></international><mapcodes><mapcode><mapcode>JL0.KP</mapcode><territory>LUX</territory></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><territory>LUX</territory></mapcode><mapcode><mapcode>SQB.NR3</mapcode><territory>BEL</territory></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><territory>BEL</territory></mapcode><mapcode><mapcode>0L46.LG9</mapcode><territory>DEU</territory></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><territory>FRA</territory></mapcode><mapcode><mapcode>VJ0L6.9PNQ</mapcode></mapcode></mapcodes></mapcodes>",
                response.readEntity(String.class));
    }

    @Test
    public void checkCodesPrecision0Json() {
        LOG.info("checkCodesPrecision0Json");
        Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/codes/" + TEST_LATLON1 + "?precision=0")).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"local\":{\"mapcode\":\"JL0.KP\",\"territory\":\"LUX\"},\"international\":{\"mapcode\":\"VJ0L6.9PNQ\"},\"mapcodes\":[{\"mapcode\":\"JL0.KP\",\"territory\":\"LUX\"},{\"mapcode\":\"R8RN.07Z\",\"territory\":\"LUX\"},{\"mapcode\":\"SQB.NR3\",\"territory\":\"BEL\"},{\"mapcode\":\"R8RN.07Z\",\"territory\":\"BEL\"},{\"mapcode\":\"0L46.LG9\",\"territory\":\"DEU\"},{\"mapcode\":\"R8RN.07Z\",\"territory\":\"FRA\"},{\"mapcode\":\"VJ0L6.9PNQ\"}]}",
                response.readEntity(String.class));

        response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/xml/codes/" + TEST_LATLON1 + "?precision=0")).
                request().
                get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcodes><local><mapcode>JL0.KP</mapcode><territory>LUX</territory></local><international><mapcode>VJ0L6.9PNQ</mapcode></international><mapcodes><mapcode><mapcode>JL0.KP</mapcode><territory>LUX</territory></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><territory>LUX</territory></mapcode><mapcode><mapcode>SQB.NR3</mapcode><territory>BEL</territory></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><territory>BEL</territory></mapcode><mapcode><mapcode>0L46.LG9</mapcode><territory>DEU</territory></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><territory>FRA</territory></mapcode><mapcode><mapcode>VJ0L6.9PNQ</mapcode></mapcode></mapcodes></mapcodes>",
                response.readEntity(String.class));
    }

    @Test
    public void checkCodesPrecision0Xml() {
        LOG.info("checkCodesPrecision0Xml");
        final Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/codes/" + TEST_LATLON1 + "?precision=0")).
                request().
                accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcodes><local><mapcode>JL0.KP</mapcode><territory>LUX</territory></local><international><mapcode>VJ0L6.9PNQ</mapcode></international><mapcodes><mapcode><mapcode>JL0.KP</mapcode><territory>LUX</territory></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><territory>LUX</territory></mapcode><mapcode><mapcode>SQB.NR3</mapcode><territory>BEL</territory></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><territory>BEL</territory></mapcode><mapcode><mapcode>0L46.LG9</mapcode><territory>DEU</territory></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><territory>FRA</territory></mapcode><mapcode><mapcode>VJ0L6.9PNQ</mapcode></mapcode></mapcodes></mapcodes>",
                response.readEntity(String.class));
    }

    @Test
    public void checkCodesPrecision1Json() {
        LOG.info("checkCodesPrecision1Json");
        final Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/codes/" + TEST_LATLON1 + "?precision=1")).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"local\":{\"mapcode\":\"JL0.KP-8\",\"territory\":\"LUX\"},\"international\":{\"mapcode\":\"VJ0L6.9PNQ-0\"},\"mapcodes\":[{\"mapcode\":\"JL0.KP-8\",\"territory\":\"LUX\"},{\"mapcode\":\"R8RN.07Z-M\",\"territory\":\"LUX\"},{\"mapcode\":\"SQB.NR3-P\",\"territory\":\"BEL\"},{\"mapcode\":\"R8RN.07Z-M\",\"territory\":\"BEL\"},{\"mapcode\":\"0L46.LG9-Q\",\"territory\":\"DEU\"},{\"mapcode\":\"R8RN.07Z-M\",\"territory\":\"FRA\"},{\"mapcode\":\"VJ0L6.9PNQ-0\"}]}",
                response.readEntity(String.class));
    }

    @Test
    public void checkCodesPrecision1Xml() {
        LOG.info("checkCodesPrecision1Xml");
        final Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/codes/" + TEST_LATLON1 + "?precision=1")).
                request().
                accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcodes><local><mapcode>JL0.KP-8</mapcode><territory>LUX</territory></local><international><mapcode>VJ0L6.9PNQ-0</mapcode></international><mapcodes><mapcode><mapcode>JL0.KP-8</mapcode><territory>LUX</territory></mapcode><mapcode><mapcode>R8RN.07Z-M</mapcode><territory>LUX</territory></mapcode><mapcode><mapcode>SQB.NR3-P</mapcode><territory>BEL</territory></mapcode><mapcode><mapcode>R8RN.07Z-M</mapcode><territory>BEL</territory></mapcode><mapcode><mapcode>0L46.LG9-Q</mapcode><territory>DEU</territory></mapcode><mapcode><mapcode>R8RN.07Z-M</mapcode><territory>FRA</territory></mapcode><mapcode><mapcode>VJ0L6.9PNQ-0</mapcode></mapcode></mapcodes></mapcodes>",
                response.readEntity(String.class));
    }

    @Test
    public void checkCodesPrecision8Json() {
        LOG.info("checkCodesPrecision8Json");
        final Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/codes/" + TEST_LATLON1 + "?precision=8")).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"local\":{\"mapcode\":\"JL0.KP-81B34315\",\"territory\":\"LUX\"},\"international\":{\"mapcode\":\"VJ0L6.9PNQ-03Q7CGV4\"},\"mapcodes\":[{\"mapcode\":\"JL0.KP-81B34315\",\"territory\":\"LUX\"},{\"mapcode\":\"R8RN.07Z-MWPCRQBK\",\"territory\":\"LUX\"},{\"mapcode\":\"SQB.NR3-P6880000\",\"territory\":\"BEL\"},{\"mapcode\":\"R8RN.07Z-MWPCRQBK\",\"territory\":\"BEL\"},{\"mapcode\":\"0L46.LG9-QWPVQVRW\",\"territory\":\"DEU\"},{\"mapcode\":\"R8RN.07Z-MWPCRQBK\",\"territory\":\"FRA\"},{\"mapcode\":\"VJ0L6.9PNQ-03Q7CGV4\"}]}",
                response.readEntity(String.class));
    }

    @Test
    public void checkCodesPrecision8Xml() {
        LOG.info("checkCodesPrecision8Xml");
        final Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/codes/" + TEST_LATLON1 + "?precision=8")).
                request().
                accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcodes><local><mapcode>JL0.KP-81B34315</mapcode><territory>LUX</territory></local><international><mapcode>VJ0L6.9PNQ-03Q7CGV4</mapcode></international><mapcodes><mapcode><mapcode>JL0.KP-81B34315</mapcode><territory>LUX</territory></mapcode><mapcode><mapcode>R8RN.07Z-MWPCRQBK</mapcode><territory>LUX</territory></mapcode><mapcode><mapcode>SQB.NR3-P6880000</mapcode><territory>BEL</territory></mapcode><mapcode><mapcode>R8RN.07Z-MWPCRQBK</mapcode><territory>BEL</territory></mapcode><mapcode><mapcode>0L46.LG9-QWPVQVRW</mapcode><territory>DEU</territory></mapcode><mapcode><mapcode>R8RN.07Z-MWPCRQBK</mapcode><territory>FRA</territory></mapcode><mapcode><mapcode>VJ0L6.9PNQ-03Q7CGV4</mapcode></mapcode></mapcodes></mapcodes>",
                response.readEntity(String.class));
    }

    @Test
    public void checkCodesLocalJson() {
        LOG.info("checkCodesLocalJson");
        Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/codes/" + TEST_LATLON2 + "/local")).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        String x = response.readEntity(String.class);
        Assert.assertEquals("{\"mapcode\":\"QKM.N4\",\"territory\":\"NLD\"}",
                x);

        response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/codes/51.427804,5.488075125/local")).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        x = response.readEntity(String.class);
        Assert.assertEquals("{\"mapcode\":\"XX.XV\",\"territory\":\"NLD\"}",
                x);

        response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/codes/51.427804,5.488075125/local?territory=NLD")).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        x = response.readEntity(String.class);
        Assert.assertEquals("{\"mapcode\":\"XX.XV\",\"territory\":\"NLD\"}",
                x);

        response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/codes/51.427804,5.488075125/local?territory=BEL")).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        x = response.readEntity(String.class);
        Assert.assertEquals("{\"mapcode\":\"5S6.4G2\",\"territory\":\"BEL\"}",
                x);
    }

    @Test
    public void checkCodesLocalXml() {
        LOG.info("checkCodesLocalXml");
        final Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/codes/" + TEST_LATLON2 + "/local")).
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
                target(server.url("/mapcode/codes/" + TEST_LATLON1 + "/International")).
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
                target(server.url("/mapcode/codes/" + TEST_LATLON1 + "/International")).
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
                target(server.url("/mapcode/codes/" + TEST_LATLON2 + "?include=offset,territory,alphabet")).
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
                target(server.url("/mapcode/codes/" + TEST_LATLON2 + "?include=offset,territory,alphabet")).
                request().
                accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcodes><local><mapcode>QKM.N4</mapcode><mapcodeInAlphabet>QKM.N4</mapcodeInAlphabet><territory>NLD</territory><territoryInAlphabet>NLD</territoryInAlphabet><offsetMeters>2.843693</offsetMeters></local><international><mapcode>VHVN4.YZ74</mapcode><mapcodeInAlphabet>VHVN4.YZ74</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>AAA</territoryInAlphabet><offsetMeters>1.907245</offsetMeters></international><mapcodes><mapcode><mapcode>QKM.N4</mapcode><mapcodeInAlphabet>QKM.N4</mapcodeInAlphabet><territory>NLD</territory><territoryInAlphabet>NLD</territoryInAlphabet><offsetMeters>2.843693</offsetMeters></mapcode><mapcode><mapcode>CZQ.376</mapcode><mapcodeInAlphabet>CZQ.376</mapcodeInAlphabet><territory>NLD</territory><territoryInAlphabet>NLD</territoryInAlphabet><offsetMeters>5.004936</offsetMeters></mapcode><mapcode><mapcode>N39J.QW0</mapcode><mapcodeInAlphabet>N39J.QW0</mapcodeInAlphabet><territory>NLD</territory><territoryInAlphabet>NLD</territoryInAlphabet><offsetMeters>2.836538</offsetMeters></mapcode><mapcode><mapcode>VHVN4.YZ74</mapcode><mapcodeInAlphabet>VHVN4.YZ74</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>AAA</territoryInAlphabet><offsetMeters>1.907245</offsetMeters></mapcode></mapcodes></mapcodes>",
                response.readEntity(String.class));
    }

    private void doCheckJson(@Nonnull final String alphabet, @Nonnull final String expected) {
        final Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/codes/" + TEST_LATLON1 + "?include=territory,alphabet&alphabet=" + alphabet)).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(expected, response.readEntity(String.class));
    }
}
