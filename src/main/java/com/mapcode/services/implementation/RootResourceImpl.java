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
            "MAPCODE API\n" +
            "-----------\n\n" +

            "GET /mapcode/codes/{lat},{lon} [/all|local|international] [?precision=[0|1|2]&territory={territory}&include={offset|territory}]\n" +
            "   Convert latitude/longitude to one or more mapcodes.\n\n" +

            "   Path parameters:\n" +
            "   lat             : latitude, range [-90, 90]\n" +
            "   lon             : longitude, range [-180, 180] (mapped if outside range)\n\n" +

            "   An additional filter can be specified to limit the results:\n" +
            "     all           : same as without specifying a filter, returns all mapcodes\n" +
            "     local         : return the shortest local mapcode\n" +
            "     international : return the shortest international mapcode\n\n" +

            "   Query parameters:\n" +
            "   precision       : precision, range [0, 2] (default=0)\n" +
            "   territory       : territory context, numeric or alpha code\n" +
            "   include         : Multiple options may be set, separated by comma's:\n" +
            "                     offset    = include offset from mapcode center to lat/lon (in meters)\n" +
            "                     territory = always include territory in result, also for territory 'AAA'\n\n" +

            "GET /mapcode/coords/{mapcode} [?territory={code}]\n" +
            "   Convert a mapcode into a latitude/longitude pair\n\n" +

            "   Path parameters:\n" +
            "   territory       : territory context, numeric or alpha code\n" +

            "   Path parameters:\n" +
            "   territory       : territory context, numeric or alpha code\n\n" +

            "GET /mapcode/territories [?offset={offset}&count={count}]\n" +
            "   Return a list of all valid numeric and alpha territory codes.\n\n" +

            "   Query parameters:\n" +
            "   offset          : return list from 'offset' (negative value start counting from end)\n" +
            "   count           : return 'count' items at most\n\n" +

            "GET /mapcode/territories/{code}\n" +
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
    public String getHelpHTML() {
        LOG.info("getHelpHTML: POM version={}", mavenProperties.getPomVersion());
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
        final VersionDTO result = new VersionDTO(pomVersion); // Create a binder.
        result.validate();                                          // You must validate it before using it.

        // Build the response and return it.
        response.setResponse(Response.ok(result).build());
    }
}
