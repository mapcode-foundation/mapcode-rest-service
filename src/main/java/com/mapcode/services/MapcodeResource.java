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

@Path("/")
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

    /**
     * This method is added only to provide a reasonable error message if you forget to specify
     * the 'type' field in the URL.
     *
     * @param paramLatDeg    Dummy.
     * @param paramLonDeg    Dummy.
     * @param paramPrecision Dummy.
     * @param paramTerritory Dummy.
     * @param response       Dummy.
     * @throws ApiException ApiException API exception, translated into HTTP status code.
     */
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

    /**
     * Convert a lat/lon to one or more mapcodes.
     *
     * @param paramLatDeg    Latitude. Range: [-90, 90].
     * @param paramLonDeg    Longitude. Range: Any double, wrapped along the earth to [-180, 180].
     * @param paramType      Specifies whether to return only the shortest local, the international, or all mapcodes.
     *                       Range: all, local, international.
     * @param paramPrecision Precision specifier; specifies additional mapcode digits. Range: [0, 2].
     * @param paramTerritory Specifies a territory context to create a local mapcode for. This is only useful for local mapcodes.
     *                       Range: any valid territory code, alpha or numeric.
     * @param paramInclude   Specifies whether to include the offset (in meters) from the mapcode center to the specified lat/lon.
     *                       Range: none, offset.
     * @param response       One or more mapcodes.
     * @throws ApiException API exception, translated into HTTP status code.
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("to/{" + PARAM_LAT_DEG + "}/{" + PARAM_LON_DEG + "}/{" + PARAM_TYPE + '}')
    void convertLatLonToMapcode(
            @PathParam(PARAM_LAT_DEG) final double paramLatDeg,
            @PathParam(PARAM_LON_DEG) final double paramLonDeg,
            @PathParam(PARAM_TYPE) @Nonnull final String paramType,
            @QueryParam(PARAM_PRECISION) @DefaultValue("0") final int paramPrecision,
            @QueryParam(PARAM_TERRITORY) @Nullable final String paramTerritory,
            @QueryParam(PARAM_INCLUDE) @DefaultValue("none") @Nonnull final String paramInclude,
            @Suspend(ApiConstants.SUSPEND_TIMEOUT) @Nonnull AsynchronousResponse response) throws ApiException;

    /**
     * Convert a mapcode into a lat/lon pair.
     *
     * @param paramMapcode   Mapcode to convert.
     * @param paramTerritory Optional territory code. Range: any valid territory code, alpha or numeric.
     * @param response       Lat/lon.
     * @throws ApiException API exception, translated into HTTP status code.
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("from/{" + PARAM_MAPCODE + '}')
    void convertMapcodeToLatLon(
            @PathParam(PARAM_MAPCODE) @Nonnull final String paramMapcode,
            @QueryParam(PARAM_TERRITORY) @Nullable final String paramTerritory,
            @Suspend(ApiConstants.SUSPEND_TIMEOUT) @Nonnull AsynchronousResponse response) throws ApiException;

    /**
     * Get a list of all valid territory codes.
     *
     * @param offset   Return values from 'offset'. Range: &gt;= 0 counts from start, &lt; 0 counts from end.
     * @param count    Return 'count' values at most. Range: &gt;= 0.
     * @param response Territory codes and information.
     * @throws ApiException API exception, translated into HTTP status code.
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("territory")
    void getTerritories(
            @QueryParam(PARAM_OFFSET) @DefaultValue(DEFAULT_OFFSET) final int offset,
            @QueryParam(PARAM_COUNT) @DefaultValue(DEFAULT_COUNT) final int count,
            @Suspend(ApiConstants.SUSPEND_TIMEOUT) @Nonnull AsynchronousResponse response) throws ApiException;

    /**
     * Get info for a specific territory.
     *
     * @param paramTerritory Territory code. Range: any valid territory code, alpha or numeric.
     * @param response       Territory information.
     * @throws ApiException API exception, translated into HTTP status code.
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("territory/{" + PARAM_TERRITORY + '}')
    void getTerritory(
            @PathParam(PARAM_TERRITORY) @Nonnull final String paramTerritory,
            @Suspend(ApiConstants.SUSPEND_TIMEOUT) @Nonnull AsynchronousResponse response) throws ApiException;
}
