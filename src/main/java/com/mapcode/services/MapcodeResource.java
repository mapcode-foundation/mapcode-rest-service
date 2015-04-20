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

package com.mapcode.services;

import com.tomtom.speedtools.apivalidation.exceptions.ApiException;
import org.jboss.resteasy.annotations.Suspend;
import org.jboss.resteasy.spi.AsynchronousResponse;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/mapcode")
public interface MapcodeResource {

    public enum ParamType {
        ALL,
        LOCAL,
        INTERNATIONAL
    }

    public enum ParamInclude {
        NONE,
        OFFSET
    }

    static final String PARAM_LAT_DEG = "lat";
    static final String PARAM_LON_DEG = "lon";
    static final String PARAM_PRECISION = "precision";
    static final String PARAM_TERRITORY = "territory";
    static final String PARAM_TYPE = "type";
    static final String PARAM_MAPCODE = "mapcode";
    static final String PARAM_INCLUDE = "include";
    static final String PARAM_COUNT = "count";
    static final String PARAM_OFFSET = "offset";

    static final String DEFAULT_OFFSET = "0";
    static final String DEFAULT_COUNT = "1000";

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("to/{" + PARAM_LAT_DEG + "}/{" + PARAM_LON_DEG + '}')
    void convertLatLonToMapcodeAll(
            @PathParam(PARAM_LAT_DEG) final double paramLatDeg,
            @PathParam(PARAM_LON_DEG) final double paramLonDeg,
            @PathParam(PARAM_PRECISION) @DefaultValue("0") final int paramPrecision,
            @QueryParam(PARAM_TERRITORY) @Nullable final String paramTerritory,
            @Suspend(ApiConstants.SUSPEND_TIMEOUT) @Nonnull AsynchronousResponse response) throws ApiException;

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("to/{" + PARAM_LAT_DEG + "}/{" + PARAM_LON_DEG + "}/{" + PARAM_TYPE + '}')
    void convertLatLonToMapcode(
            @PathParam(PARAM_LAT_DEG) final double paramLatDeg,
            @PathParam(PARAM_LON_DEG) final double paramLonDeg,
            @PathParam(PARAM_PRECISION) @DefaultValue("0") final int paramPrecision,
            @PathParam(PARAM_TYPE) @Nonnull final String paramType,
            @QueryParam(PARAM_TERRITORY) @Nullable final String paramTerritory,
            @QueryParam(PARAM_INCLUDE) @DefaultValue("none") @Nonnull final String paramInclude,
            @Suspend(ApiConstants.SUSPEND_TIMEOUT) @Nonnull AsynchronousResponse response) throws ApiException;

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("from/{" + PARAM_MAPCODE + '}')
    void convertMapcodeToLatLon(
            @PathParam(PARAM_MAPCODE) @Nonnull final String paramMapcode,
            @QueryParam(PARAM_TERRITORY) @Nullable final String paramTerritory,
            @Suspend(ApiConstants.SUSPEND_TIMEOUT) @Nonnull AsynchronousResponse response) throws ApiException;

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("territory")
    void getTerritories(
            @QueryParam(PARAM_OFFSET) @DefaultValue(DEFAULT_OFFSET) final int offset,
            @QueryParam(PARAM_COUNT) @DefaultValue(DEFAULT_COUNT) final int count,
            @Suspend(ApiConstants.SUSPEND_TIMEOUT) @Nonnull AsynchronousResponse response) throws ApiException;

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("territory/{" + PARAM_TERRITORY + '}')
    void getTerritory(
            @PathParam(PARAM_TERRITORY) @Nonnull final String paramTerritory,
            @Suspend(ApiConstants.SUSPEND_TIMEOUT) @Nonnull AsynchronousResponse response) throws ApiException;
}
