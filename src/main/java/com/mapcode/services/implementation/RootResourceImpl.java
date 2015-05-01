/*
 * Copyright (C) 2015 Stichting Mapcode Foundation (http://www.mapcode.com)
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

import com.google.common.io.BaseEncoding;
import com.mapcode.services.ApiConstants;
import com.mapcode.services.RootResource;
import com.mapcode.services.dto.VersionDTO;
import com.tomtom.speedtools.maven.MavenProperties;
import org.jboss.resteasy.annotations.Suspend;
import org.jboss.resteasy.spi.AsynchronousResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

/**
 * This class implements the REST API that deals with TTBin files.
 */
public class RootResourceImpl implements RootResource {
    private static final Logger LOG = LoggerFactory.getLogger(RootResourceImpl.class);

    @Nonnull
    private static final String HELP_TEXT = "" +
            "MAPCODE REST SERVICES\n" +
            "---------------------\n\n" +

            "GET /mapcode/to/{lat}/{lon}/[all|local|international] [?precision=[0|1|2]&territory={territory}&include={none|offset}]\n" +
            "   Convert latitude/longitude to one or more mapcodes.\n\n" +

            "   Path parameters:\n" +
            "   lat             : latitude, range [-90, 90]\n" +
            "   lon             : longitude, range [-180, 180] (mapped if outside range)\n" +
            "   all             : return all mapcodes (sorted short to long)\n" +
            "   local           : return the shortest local mapcode\n" +
            "   international   : return the shortest international mapcode\n\n" +

            "   Query parameters:\n" +
            "   precision       : precision, range [0, 2] (default=0)\n" +
            "   territory       : territory context, numeric or alpha code\n" +
            "   include         : include offset from mapcode center to lat/lon (in meters)\n\n" +

            "GET /mapcode/from/{mapcode} [?territory={code}]\n" +
            "   Convert a mapcode into a latitude/longitude pair\n\n" +

            "   Path parameters:\n" +
            "   territory       : territory context, numeric or alpha code\n" +

            "   Path parameters:\n" +
            "   territory       : territory context, numeric or alpha code\n" +

            "GET /mapcode/territory [?offset={offset}&count={count}]\n" +
            "   Return a list of all valid numeric and alpha territory codes.\n\n" +

            "   Query parameters:\n" +
            "   offset          : return list from 'offset' (negative value start counting from end)\n" +
            "   count           : return 'count' items at most\n\n" +

            "GET /mapcode/territory/{code}\n" +
            "   Return information for a single territory code.\n\n" +

            "   Path parameters:\n" +
            "   territory       : territory context, numeric or alpha code\n";

    @Nonnull
    private final MavenProperties mavenProperties;

    @Inject
    public RootResourceImpl(
            @Nonnull final MavenProperties mavenProperties) {
        assert mavenProperties != null;

        // Store the injected values.
        this.mavenProperties = mavenProperties;
    }

    @Override
    @Nonnull
    public String getRoot() {
        return getHelp();
    }

    @Override
    @Nonnull
    public String getHelp() {
        return "<html><pre>\n" + HELP_TEXT + "</pre></html>\n";
    }

    /**
     * This is the actual REST API call. Note that the response is an asynchronous response. The response type is
     * entirely type safe and validated in the corresponding "binder" object in the package "domain" (the use of names
     * like binder and domain is convention only).
     */
    @Override
    public void getVersion(
            @Nonnull @Suspend(ApiConstants.SUSPEND_TIMEOUT) final AsynchronousResponse response) {
        assert response != null;

        // No input validation required. Just return version number.
        final String pomVersion = mavenProperties.getPomVersion();
        LOG.info("getVersion: POM version={}", pomVersion);

        // Create the response binder and validate it (returned objects must also be validated!).
        // Validation errors are automatically caught as exceptions and returned by the framework.
        final VersionDTO binder = new VersionDTO(pomVersion); // Create a binder.
        binder.validate();                                          // You must validate it before using it.

        // Build the response and return it.
        response.setResponse(Response.ok(binder).build());
    }

    @Override
    @Nonnull
    public byte[] getFavicon() {

        final String faviconBase64 = "" +
                "AAABAAEAEBAAAAEACABoBQAAFgAAACgAAAAQAAAAIAAAAAEACAAAAAAAAAEAAAAAAAAAAAAAAAEA" +
                "AAAAAAAAAAAAGhYLABsWCwAyKxwA3eLrAPj6/AC4v8wAb3BqACgiFAApIhQAUU1CAFNMPwCIi40A" +
                "iIyQAEQ9LgB6fHwAMSsdAEpCNADi5u8ATEc6AE1HOgBgXVcAPzgpABQRBwCKiosALCYYAHZ3dwCt" +
                "tcIAMCobAFhSSQBFPjIABAMCAEpCNQBxbWMApqu3AIOFhgCXnKYAdnRvACwlFgB1d3gAmqCpAImP" +
                "kgACAgAAjJCVAJ2lrwBma2cASUEzAFxWTQBLRTYATEU2AJSXmABKRDwAIx0RALzBzACWm6QAUEo8" +
                "ACghFADS2OMA0dnmAFJKPACDiZMAh4uQAEI7KwAYFAkABAIBAGxpYgBHQTQANC0dAH2BhQBJRjoA" +
                "4ujvADkyIwClrrkA5unyADw2JgCpsrwAc3V0AD43KQBiYV0AVU9DAFRRSQCfpK4AMy0eAAYFAgCn" +
                "rroAJyATAP///gCpsboAKSQWAEA3KgB0dngAdnh1AFVPRABlY2QAwsrXAFdPRABraWQAjpSbAHyA" +
                "hABcVU0Aj5SbAKKptQB/gIQAODEiAAwIAwBeWVAAXlpTAGBaUwCChYoAvcPMAP///wASDwYAlp6n" +
                "AJiepwBBOisAZ2RfAFZSRQBFPzEAamhiAI6SlgBaVEsAHxcMAFpVTgB6gosAXVlRAIGEiADP1uEA" +
                "UEg6AHVzcQB0dHQAZWJaAMHJ1gBFPi8ARj8yALK6xQCOkpcAXFVGAKCotABbV0wAXFhPAG5taQBc" +
                "WVIAcW9mAIGEiQD9/v4A/v7+AGNcUgBiXVUAc3NyAEA5KgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
                "AAAAUm8RQgJ9U4srOZFubm5ublyVAIxHRmgWGn4Sbm5ubm6ACziECIVqUYoQckBWbm5uKBVUGXxn" +
                "bIlBDi+UG25ubgQ9YTQBOg8kLhxpMSdubm6RPkgJWjYlcU11DnZkbm5uboNlLH81PI9gE3M7TG5u" +
                "bm6QKnAHMGZQICZ3S25ubm5ubnldIl9DNwOCDI1ubm5ubm5Yay1FdD9PYiltDW5ubm5uP457ThR4" +
                "FzJZVW5ubm5ubkkfhh6SCh1bGAZubm5ubm5uSoghM2Mjeldubm5ubm5ubm6Hk4FEXgVubm5ubm5u" +
                "bm5ubm5ubm5ubm5ubm5ubm5ubm5ubm5ubm5ubm5ubgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
        final byte[] faviconDecoded = BaseEncoding.base64().decode(faviconBase64);
        return faviconDecoded;
    }
}
