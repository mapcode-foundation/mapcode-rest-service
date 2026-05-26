/*
 * Copyright (C) 2016-2026, Stichting Mapcode Foundation (http://www.mapcode.com)
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

public class ApiCodesTerritoriesTest {
    private static final Logger LOG = LoggerFactory.getLogger(ApiCodesTerritoriesTest.class);

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
    public void pointInsideCountryReturnsCountryJson() {
        // (52, 5) is inside the NLD fixture polygon.
        // TerritoryCandidateListDTO extends ApiListDTO which serializes as a bare JSON array.
        final Response response = new ResteasyClientBuilder().build()
                .target(server.url("/mapcode/codes/52.0,5.0/territories"))
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(
                "[{\"alphaCode\":\"NLD\"}]",
                response.readEntity(String.class));
    }

    @Test
    public void pointInsideSubdivisionReturnsSubdivisionThenCountryJson() {
        // (36, -120) is inside the USA-CA fixture polygon, which is inside USA.
        // Subdivision comes first, then parent country.
        final Response response = new ResteasyClientBuilder().build()
                .target(server.url("/mapcode/codes/36.0,-120.0/territories"))
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(
                "[" +
                        "{\"alphaCode\":\"USA-CA\",\"parentAlphaCode\":\"USA\"}," +
                        "{\"alphaCode\":\"USA\"}]",
                response.readEntity(String.class));
    }

    @Test
    public void pointAtSeaReturnsEmptyListJson() {
        // (0, -30) mid-Atlantic — no fixture polygon contains it.
        // ApiListDTO with NON_EMPTY serializes an empty list as [].
        final Response response = new ResteasyClientBuilder().build()
                .target(server.url("/mapcode/codes/0.0,-30.0/territories"))
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("[]", response.readEntity(String.class));
    }

    @Test
    public void disputedRegionReturnsSmallerPolygonFirstJson() {
        // (6.5, 106.5) is inside both DISPUTED-A (large) and DISPUTED-B (small); B first.
        final Response response = new ResteasyClientBuilder().build()
                .target(server.url("/mapcode/codes/6.5,106.5/territories"))
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(
                "[" +
                        "{\"alphaCode\":\"DISPUTED-B\"}," +
                        "{\"alphaCode\":\"DISPUTED-A\"}]",
                response.readEntity(String.class));
    }

    @Test
    public void latOutOfRangeReturns400() {
        final Response response = new ResteasyClientBuilder().build()
                .target(server.url("/mapcode/codes/91.0,5.0/territories"))
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertEquals(400, response.getStatus());
    }

    @Test
    public void lonOutOfRangeIsWrapped() {
        // 365 wraps to 5 → inside NLD.
        final Response response = new ResteasyClientBuilder().build()
                .target(server.url("/mapcode/codes/52.0,365.0/territories"))
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(
                "[{\"alphaCode\":\"NLD\"}]",
                response.readEntity(String.class));
    }

    @Test
    public void pointInsideCountryReturnsCountryXml() {
        // ApiListDTO does not serialize individual elements via JAXB XmlElement annotations;
        // the XML representation is a self-closing <territories/> tag regardless of contents.
        final Response response = new ResteasyClientBuilder().build()
                .target(server.url("/mapcode/codes/52.0,5.0/territories"))
                .request()
                .accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><territories/>",
                response.readEntity(String.class));
    }
}
