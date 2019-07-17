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

package com.mapcode.services.implementation;

import com.mapcode.services.MapcodeResource;
import com.mapcode.services.RootResource;
import com.mapcode.services.dto.MapcodeDTO;
import com.mapcode.services.dto.PointDTO;
import com.mapcode.services.dto.VersionDTO;
import com.mapcode.services.metrics.SystemMetrics;
import com.tomtom.speedtools.json.Json;
import com.tomtom.speedtools.maven.MavenProperties;
import com.tomtom.speedtools.time.UTCTime;
import com.tomtom.speedtools.utils.MathUtils;
import org.joda.time.DateTime;
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
            "IMPORTANT NOTICE: The Mapcode Foundation provides an implementation of the Mapcode REST API at:\n" +
            "----------------- https://api.mapcode.com/mapcode\n\n" +

            "                  This free online service is provided for demonstration purposes only and the Mapcode\n" +
            "                  Foundation accepts no claims on its availability or reliability, although we'll try hard\n" +
            "                  to provide a stable and decent service. Note that usage or log data may be collected, only\n" +
            "                  to further improve service (never for commercial purposes). If you are interested in using\n" +
            "                  the service for professional purposes, or in high-availability or high-demands contexts,\n" +
            "                  you may wish to consider self-hosting this service. The source code is open-source and\n" +
            "                  available at: https://github.com/mapcode-foundation/mapcode-rest-service\n\n" +

            "The REST API Methods\n" +
            "--------------------\n\n" +

            "All REST services (except 'metrics') are able to return both JSON and XML. Use the HTTP\n" +
            "'Accept:' header to specify the expected format: application/json or application/xml\n" +
            "If the 'Accept:' header is omitted, JSON is assumed.\n\n" +

            "GET /mapcode         Returns this help page.\n" +
            "GET /mapcode/version Returns the software version.\n" +
            "GET /mapcode/metrics Returns some system metrics (JSON-only, also available from JMX).\n" +
            "GET /mapcode/status  Returns 200 if the service OK.\n\n" +

            "GET /mapcode/codes/{lat},{lon}[/[mapcodes|local|international]]\n" +
            "     [?precision=[0..8] & territory={restrictToTerritory} & country={restrictToCountry}\n" +
            "     alphabet={alphabet} & include={offset|territory|alphabet|rectangle}]\n\n" +

            "   Convert latitude/longitude to one or more mapcodes. The response always contains the 'international' mapcode and\n" +
            "   only contains a 'local' mapcode if there are any non-international mapcode AND they are all of the same territory.\n\n" +

            "   The 'country' parameter always specifies a country, by a 2 or 3 character ISO-3166 code, like 'US' or 'USA'\n" +
            "   (for the USA), and 'NL' or 'NLD' (for the Netherlands). In a web environment, the country code is often available\n" +
            "   as a 2-character code. That code can be used for this parameter.\n\n" +

            "   The 'territory' parameter is a 2, 3 or 5 (XX-YY) character code. These code can be countries or states within countries.\n" +
            "   Some 2 character state codes are the same as country codes. In that case, the territory implies the state, not the country.\n" +
            "   For example, the territory code 'US' is unambiguous and means USA, but 'NL' means 'IN-NL' (Nagaland, India) rather than\n" +
            "   the Netherlands. You cannot use the standard 2-character country codes in web applications for this parameter.\n\n" +

            "   Path parameters:\n" +
            "     lat             : Latitude, range [-90, 90] (automatically limited to this range).\n" +
            "     lon             : Longitude, range [-180, 180] (automatically wrapped to this range).\n\n" +

            "   An additional filter can be specified to limit the results:\n" +
            "     mapcodes        : Same as without specifying a filter, returns all mapcodes.\n" +
            "     local           : Return the shortest local mapcode (not an international code). Note that multiple local\n" +
            "                       mapcodes may exist for a location, with different territories. This method returns the\n" +
            "                       shortest code. It does not check if the territory is the 'geographically correct' one\n" +
            "                       for the coordinates. To get the shortest code for a specific territory, you need to explicitly\n" +
            "                       specify the territory with 'territory=' parameter in the query.\n" +
            "     international   : Return the international mapcode.\n\n" +

            "   Query parameters:\n" +
            "     precision       : Precision, range [0..8] (default=0).\n" +
            "     territory       : Territory (country or state) to restrict results to (name or alphacode).\n" +
            "     country         : Country to restrict results to (name or alphacode).\n" +
            "     alphabet        : Alphabet to return results in.\n" +
            "     include         : Multiple options may be set, separated by comma's:\n" +
            "                         offset    = Include offset from mapcode center to lat/lon (in meters).\n" +
            "                         territory = Always include territory in result, also for territory 'AAA'.\n" +
            "                         alphabet  = Always the mapcodeInAlphabet, also if same as mapcode.\n" +
            "                         rectangle = Include the encompassing rectangle of a mapcode.\n\n" +

            "                       Note that you can use 'include=territory,alphabet' to ensure the territory code\n" +
            "                       is always present, as well as the translated territory and mapcode codes.\n" +
            "                       This can make processing the records easier in scripts, for example.\n\n" +

            "GET /mapcode/coords/{code} [?context={territory} & include={include}]\n" +
            "   Convert a mapcode into a latitude/longitude pair.\n\n" +

            "   Path parameters:\n" +
            "     code            : Mapcode code (local or international). You can specify the territory in the code itself,\n" +
            "                       like 'NLD%20XX.XX' (note that the space is URL-encoded to '%20'), or you specifty the\n" +
            "                       territory separately in the 'context=' parameter, like 'XX.XX?context-NLD'.\n" +

            "   Query parameters:\n" +
            "     context         : Optional mapcode territory context (name or alphacode). The context is only used if the\n\n" +
            "                       code is ambiguous without it, otherwise it is ignored. For example, the context is ignored\n" +
            "                       when converting an international code (but it is not considered an error to provide it).\n" +
            "     include         : An additional option may be set:\n" +
            "                         rectangle = Include the encompassing rectangle of a mapcode.\n\n" +

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

            "JSON and XML Responses\n" +
            "----------------------\n\n" +

            "The REST API methods defined above obey the HTTP \"Accept:\" header. To retrieve JSON responses,\n" +
            "use \"Accept:application/json\", to retrieve XML responses, use \"Accept:application/xml\".\n\n" +

            "The default response type is JSON, if no \"Accept:\" header is specified.\n\n" +

            "Note that some browsers (such as FireFox) may silently insert an 'Accept:' header when using a\n" +
            "XMLHttpRequest in Javascript. This may lead to returning JSON in some browsers and XML in others.\n" +
            "It is advised to explicitly set the appropriate header in Javascript in such cases.\n\n" +

            "Alternatively, to retrieve XML or JSON responses if no \"Accept:\" header is specified, you can add\n" +
            "\"/xml\" or \"/json\" in the URL, directly after \"/mapcode\".\n\n" +

            "So, the following methods are supported as well and return XML or JSON by default:\n\n" +

            "    GET /mapcode/xml/version           GET /mapcode/json/version\n" +
            "    GET /mapcode/xml/status            GET /mapcode/json/status\n" +
            "    GET /mapcode/xml/codes             GET /mapcode/json/codes\n" +
            "    GET /mapcode/xml/coords            GET /mapcode/json/coords\n" +
            "    GET /mapcode/xml/territories       GET /mapcode/json/territories\n" +
            "    GET /mapcode/xml/alphabets         GET /mapcode/json/alphabets\n";

    private final MapcodeResource mapcodeResource;
    private final MavenProperties mavenProperties;
    private final SystemMetrics metrics;

    @Inject
    public RootResourceImpl(
            @Nonnull final MapcodeResource mapcodeResource,
            @Nonnull final MavenProperties mavenProperties,
            @Nonnull final SystemMetrics metrics) {
        assert mapcodeResource != null;
        assert mavenProperties != null;
        assert metrics != null;

        // Store the injected values.
        this.mapcodeResource = mapcodeResource;
        this.mavenProperties = mavenProperties;
        this.metrics = metrics;
    }

    @Override
    @Nonnull
    public String getHelpHTML() {
        LOG.info("getHelpHTML: show help page, version {}", mavenProperties.getPomVersion());
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
    public void getStatus(@Suspended @Nonnull final AsyncResponse response) {
        assert response != null;
        LOG.info("getStatus: get status");

        try {
            // Execute a lat/lon to mapcode conversion
            final double latDeg = 52.158974;
            final double lonDeg = 4.492479;
            final String mapcode = "QJM.1G";
            final String territory = "NLD";
            final String alphabet = null;
            final int precision = 0;
            final String include = "";
            final String client = "";
            final String allowLog = "false";
            final TestAsyncResponse asyncResponse1 = new TestAsyncResponse();
            mapcodeResource.convertLatLonToMapcode(String.valueOf(latDeg), String.valueOf(lonDeg), "local",
                    String.valueOf(precision), territory, null, null, alphabet, include, client,
                    allowLog, asyncResponse1);
            waitForResponse(asyncResponse1);

            // Check if conversion was OK.
            if (asyncResponse1.getResponse() instanceof Response) {
                final Response response1 = (Response) asyncResponse1.getResponse();
                if (response1.getEntity() instanceof MapcodeDTO) {
                    final MapcodeDTO result1 = (MapcodeDTO) response1.getEntity();
                    final MapcodeDTO expected1 = new MapcodeDTO(mapcode, null, territory, null);
                    //noinspection ConstantConditions
                    if (expected1.getMapcode().equals(result1.getMapcode()) &&
                            expected1.getTerritory().equals(result1.getTerritory())) {

                        // Now execute a mapcode to lat/lon conversion.
                        final TestAsyncResponse asyncResponse2 = new TestAsyncResponse();
                        mapcodeResource.convertMapcodeToLatLon(mapcode, territory,
                                null, include, client, allowLog, asyncResponse2);
                        waitForResponse(asyncResponse2);

                        if (asyncResponse2.getResponse() instanceof Response) {
                            final Response response2 = (Response) asyncResponse2.getResponse();
                            if (response2.getEntity() instanceof PointDTO) {
                                final PointDTO result2 = (PointDTO) response2.getEntity();
                                final PointDTO expected2 = new PointDTO(latDeg, lonDeg);
                                if (MathUtils.isAlmostEqual(expected2.getLatDeg(), result2.getLatDeg()) &&
                                        MathUtils.isAlmostEqual(expected2.getLonDeg(), result2.getLonDeg())) {

                                    // Yes, all is OK.
                                    response.resume(Response.ok().build());
                                    return;
                                }
                            }
                        }
                        response.resume(Response.ok().build());
                        return;
                    }
                }
            }
        } catch (final InterruptedException ignored) {
            // Empty.
        }

        // This are not OK.
        response.resume(Response.serverError().build());
    }

    @Override
    public void getMetrics(@Suspended @Nonnull final AsyncResponse response) {
        assert response != null;
        LOG.info("getMetrics");

        // No input validation required. Just return metrics as a plain JSON string.
        final String json = Json.toJson(metrics);
        response.resume(Response.ok(json).build());
    }

    private static void waitForResponse(@Nonnull  final TestAsyncResponse asyncResponse) throws InterruptedException {
        final int sleepMillis = 100;                            // No more than 10 reqs/sec.
        final DateTime now = UTCTime.now();
        final DateTime until = now.plusSeconds(10);             // Wait at most 10 seconds.
        while (now.isBefore(until)) {
            if (asyncResponse.isReady()) {
                if (asyncResponse.getResponse() != null) {      // If the object is null, retry.

                    // All OK.
                    return;
                }
            }
            //noinspection BusyWait
            Thread.sleep(sleepMillis);
        }
        throw new IllegalStateException("Did not receive a response");
    }
}
