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

package com.mapcode.services.implementation;

import com.mapcode.services.RootResource;
import com.mapcode.services.SystemMetrics;
import com.mapcode.services.dto.VersionDTO;
import com.tomtom.speedtools.json.Json;
import com.tomtom.speedtools.maven.MavenProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;

/**
 * This class implements the REST API that deals with the root resource for the Mapcode REST API.
 * This includes methods to get HTML help, the service version and server status.
 */
public class RootResourceImpl implements RootResource {
    private static final Logger LOG = LoggerFactory.getLogger(RootResourceImpl.class);

    @Nonnull
    private static final String HELP_TEXT = "" +
            "All REST services (except 'metrics') are able to return both JSON and XML. Use the HTTP\n" +
            "'Accept:' header to specify the expected format: application/json or application/xml\n" +
            "If the 'Accept:' header is omitted, JSON is assumed." +

            "GET /mapcode         Returns this help page.\n" +
            "GET /mapcode/version Returns the software version.\n" +
            "GET /mapcode/metrics Returns some system metrics (JSON-only, also available from JMX).\n" +
            "GET /mapcode/status  Returns 200 if the service OK.\n\n" +

            "GET /mapcode/codes/{lat},{lon}[/[mapcodes|local|international]]\n" +
            "     [?precision=[0..8] & territory={restrictToTerritory} & alphabet={alphabet} & include={offset|territory|alphabet}]\n\n" +

            "   Convert latitude/longitude to one or more mapcodes. The response always contains the 'international' mapcode and\n" +
            "   only contains a 'local' mapcode if there are any non-international mapcode AND they are all of the same territory.\n\n" +

            "   Path parameters:\n" +
            "     lat             : Latitude, range [-90, 90] (automatically limited to this range).\n" +
            "     lon             : Longitude, range [-180, 180] (automatically wrapped to this range).\n\n" +

            "   An additional filter can be specified to limit the results:\n" +
            "     mapcodes        : Same as without specifying a filter, returns all mapcodes.\n" +
            "     local           : Return the shortest local mapcode. This produces a 404 if no (unambiguous) local mapcode exists.\n" +
            "     international   : Return the international mapcode.\n\n" +

            "   Query parameters:\n" +
            "     precision       : Precision, range [0..8] (default=0).\n" +
            "     territory       : Territory to restrict results to (name or alphacode).\n" +
            "     alphabet        : Alphabet to return results in.\n\n" +

            "     include         : Multiple options may be set, separated by comma's:\n" +
            "                         offset    = Include offset from mapcode center to lat/lon (in meters).\n" +
            "                         territory = Always include territory in result, also for territory 'AAA'.\n" +
            "                         alphabet  = Always the mapcodeInAlphabet, also if same as mapcode.\n\n" +

            "                       Note that you can use 'include=territory,alphabet' to ensure the territory code\n" +
            "                       is always present, as well as the translated territory and mapcode codes.\n" +
            "                       This can make processing the records easier in scripts, for example.\n\n" +

            "GET /mapcode/coords/{code} [?context={territory}]\n" +
            "   Convert a mapcode into a latitude/longitude pair.\n\n" +

            "   Path parameters:\n" +
            "     code            : Mapcode code (local or international).\n" +

            "   Query parameters:\n" +
            "     context         : Optional mapcode territory context (name or alphacode).\n\n" +

            "GET /mapcode/territories [?offset={offset}&count={count}]\n" +
            "   Return a list of all territories.\n\n" +

            "GET /mapcode/territories/{territory} [?context={territory}]\n" +
            "   Return information for a single territory code.\n\n" +

            "   Path parameters:\n" +
            "     territory       : Territory to get info for (name or alphacode).\n\n" +

            "   Query parameters:\n" +
            "     context         : Territory context (optional, for disambiguation, name or alphacode).\n" +
            "                       The context can only be: USA IND CAN AUS MEX BRA RUS CHN ATA\n\n" +

            "GET /mapcode/alphabets [?offset={offset}&count={count}]\n" +
            "   Return a list of all alphabet codes.\n\n" +

            "GET /mapcode/alphabets/{alphabet}\n" +
            "   Return information for a specific alphabet.\n\n" +

            "   Path parameters:\n" +
            "     alphabet        : Alphabet to get info for.\n\n" +

            "General query parameters for methods which return a list of results:\n\n" +
            "   offset            : Return list from 'offset' (negative value start counting from end).\n" +
            "   count             : Return 'count' items at most.\n\n" +

            "The REST API methods defined above obey the HTTP \"Accept:\" header. To retrieve JSON responses,\n" +
            "use \"Accept:application/json\", to retrieve XML responses, use \"Accept:application/xml\".\n\n" +

            "The default response type is **JSON**, if no \"Accept:\" header is specified.\n\n" +

            "Alternatively, to retrieve **XML** responses if no \"Accept:\" header is specified, you can add \"/xml\"\n" +
            "in the URL, directly after \"/mapcode\".\n\n" +

            "So, the following methods are supported as well and return XML by default:\n\n" +

            "    GET /mapcode/xml/version\n" +
            "    GET /mapcode/xml/status\n" +
            "    GET /mapcode/xml/codes\n" +
            "    GET /mapcode/xml/coords\n" +
            "    GET /mapcode/xml/territories\n" +
            "    GET /mapcode/xml/alphabets\n\n" +

            "We recommend to use the method without \"/xml: and use the \"Accept:\" header to specify the\n" +
            "response type, however.\n";

    private final MavenProperties mavenProperties;
    private final SystemMetrics metrics;

    @Inject
    public RootResourceImpl(
            @Nonnull final MavenProperties mavenProperties,
            @Nonnull final SystemMetrics metrics) {
        assert mavenProperties != null;

        // Store the injected values.
        this.mavenProperties = mavenProperties;
        this.metrics = metrics;
    }

    @Override
    @Nonnull
    public String getHelpHTML() {
        LOG.info("getHelpHTML: show help page", mavenProperties.getPomVersion());
        return "<html><pre>\n" +
                "MAPCODE API (" + mavenProperties.getPomVersion() + ")\n" +
                "-----------\n\n" +
                HELP_TEXT + "</pre></html>\n";
    }

    @Override
    public void getVersion(@Suspended @Nonnull final AsyncResponse response) {
        assert response != null;

        // No input validation required. Just return version number.
        final String pomVersion = mavenProperties.getPomVersion();
        LOG.info("getVersion: POM version={}", pomVersion);

        // Create the response binder and validate it (returned objects must also be validated!).
        // Validation errors are automatically caught as exceptions and returned by the framework.
        final VersionDTO result = new VersionDTO(pomVersion); // Create a binder.
        result.validate();                                    // You must validate it before using it.

        // Build the response and return it.
        response.resume(Response.ok(result).build());
    }

    @Override
    public void getVersionXml(@Suspended @Nonnull AsyncResponse response) {
        getVersion(response);
    }

    @Override
    public void getStatus(@Suspended @Nonnull final AsyncResponse response) {
        assert response != null;
        LOG.info("getStatus: get status");
        response.resume(Response.ok().build());
    }

    @Override
    public void getStatusXml(@Suspended @Nonnull AsyncResponse response) {
        getStatus(response);
    }

    @Override
    public void getMetrics(@Suspended @Nonnull final AsyncResponse response) {
        assert response != null;
        LOG.info("getMetrics");

        // No input validation required. Just return metrics as a plain JSON string.
        final String json = Json.toJson(metrics);
        response.resume(Response.ok(json).build());
    }
}
