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

/**
 * This class handle the Mapcode REST API, which includes conversions to and from mapcodes.
 */
@Path("/mapcode")
public interface MapcodeResource {

    enum ParamType {
        MAPCODES,           // All mapcodes, sorted from (shortest) local to (longest) international code.
        LOCAL,              // Shortest local mapcode, which potentially requires a territory code.
        INTERNATIONAL       // Longest international mapcode, which requires not territory code.
    }

    enum ParamInclude {
        OFFSET,             // Includes offset (in meters) from center of mapcode to originally specified lat/lon.
        TERRITORY           // Force including the territory, even when the territory code is "AAA".
    }

    /**
     * Strings used as path or url parameters.
     */
    static final String PARAM_LAT_DEG = "lat";
    static final String PARAM_LON_DEG = "lon";
    static final String PARAM_PRECISION = "precision";
    static final String PARAM_TERRITORY = "territory";
    static final String PARAM_PARENT = "parent";
    static final String PARAM_CONTEXT = "context";
    static final String PARAM_TYPE = "type";
    static final String PARAM_MAPCODE = "mapcode";
    static final String PARAM_INCLUDE = "include";
    static final String PARAM_COUNT = "count";
    static final String PARAM_OFFSET = "offset";

    static final String DEFAULT_OFFSET = "0";
    static final String DEFAULT_COUNT = "1000";

    // Unsupported operation.
    @GET
    @Path("codes")
    void convertLatLonToMapcode(
            @Suspend(ApiConstants.SUSPEND_TIMEOUT) @Nonnull AsynchronousResponse response) throws ApiException;

    /**
     * Convert a lat/lon to one or more mapcodes. All possible mapcodes are returned.
     *
     * @param paramLatDeg    Latitude. Range: [-90, 90].
     * @param paramLonDeg    Longitude. Range: Any double, wrapped along the earth to [-180, 180].
     * @param paramPrecision Precision specifier; specifies additional mapcode digits. Range: [0, 2].
     * @param paramTerritory Specifies a territory context to create a local mapcode for. This is only useful for local mapcodes.
     *                       If the mapcode cannot be created for the territory, an exception is thrown.
     *                       Range: any valid territory code, alpha or numeric.
     * @param paramInclude   Specifies whether to include the offset (in meters) from the mapcode center to the specified lat/lon.
     *                       Range: {@link ParamInclude}.
     * @param response       One or more mapcodes. Format: {@link com.mapcode.services.dto.MapcodeDTO} for LOCAL and
     *                       INTERNATIONAL and {@link com.mapcode.services.dto.MapcodesDTO} for ALL.
     * @throws ApiException API exception, translated into HTTP status code.
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("codes/{" + PARAM_LAT_DEG + "},{" + PARAM_LON_DEG + '}')
    void convertLatLonToMapcode(
            @PathParam(PARAM_LAT_DEG) final double paramLatDeg,
            @PathParam(PARAM_LON_DEG) final double paramLonDeg,
            @QueryParam(PARAM_PRECISION) @DefaultValue("0") final int paramPrecision,
            @QueryParam(PARAM_TERRITORY) @Nullable final String paramTerritory,
            @QueryParam(PARAM_INCLUDE) @DefaultValue("") @Nonnull final String paramInclude,
            @Suspend(ApiConstants.SUSPEND_TIMEOUT) @Nonnull AsynchronousResponse response) throws ApiException;

    /**
     * Convert a lat/lon to one or more mapcodes.
     *
     * @param paramLatDeg    Latitude. Range: [-90, 90].
     * @param paramLonDeg    Longitude. Range: Any double, wrapped along the earth to [-180, 180].
     * @param paramType      Specifies whether to return only the shortest local, the international, or all mapcodes.
     *                       Range: {@link ParamType}, if null, no type is supplied.
     * @param paramPrecision Precision specifier; specifies additional mapcode digits. Range: [0, 2].
     * @param paramTerritory Specifies a territory context to create a local mapcode for. This is only useful for local mapcodes.
     *                       If the mapcode cannot be created for the territory, an exception is thrown.
     *                       Range: any valid territory code, alpha or numeric.
     * @param paramInclude   Specifies whether to include the offset (in meters) from the mapcode center to the specified lat/lon.
     *                       Range: {@link ParamInclude}.
     * @param response       One or more mapcodes. Format: {@link com.mapcode.services.dto.MapcodeDTO} for LOCAL and
     *                       INTERNATIONAL and {@link com.mapcode.services.dto.MapcodesDTO} for ALL.
     * @throws ApiException API exception, translated into HTTP status code.
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("codes/{" + PARAM_LAT_DEG + "},{" + PARAM_LON_DEG + "}/{" + PARAM_TYPE + '}')
    void convertLatLonToMapcode(
            @PathParam(PARAM_LAT_DEG) final double paramLatDeg,
            @PathParam(PARAM_LON_DEG) final double paramLonDeg,
            @PathParam(PARAM_TYPE) @Nullable final String paramType,
            @QueryParam(PARAM_PRECISION) @DefaultValue("0") final int paramPrecision,
            @QueryParam(PARAM_TERRITORY) @Nullable final String paramTerritory,
            @QueryParam(PARAM_INCLUDE) @DefaultValue("") @Nonnull final String paramInclude,
            @Suspend(ApiConstants.SUSPEND_TIMEOUT) @Nonnull AsynchronousResponse response) throws ApiException;

    // Unsupported operation.
    @GET
    @Path("coords")
    void convertMapcodeToLatLon(
            @Suspend(ApiConstants.SUSPEND_TIMEOUT) @Nonnull AsynchronousResponse response) throws ApiException;

    /**
     * Convert a mapcode into a lat/lon pair.
     *
     * @param paramMapcode Mapcode to convert.
     * @param paramContext Specifies a parent territory context for interpretation of the mapcode.
     *                     Range: any valid parent territory code.
     * @param response     Lat/lon. Format: {@link com.mapcode.services.dto.PointDTO}.
     * @throws ApiException API exception, translated into HTTP status code.
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("coords/{" + PARAM_MAPCODE + '}')
    void convertMapcodeToLatLon(
            @PathParam(PARAM_MAPCODE) @Nonnull final String paramMapcode,
            @QueryParam(PARAM_CONTEXT) @Nullable final String paramContext,
            @Suspend(ApiConstants.SUSPEND_TIMEOUT) @Nonnull AsynchronousResponse response) throws ApiException;

    /**
     * Get a list of all valid territory codes.
     *
     * @param offset   Return values from 'offset'. Range: &gt;= 0 counts from start, &lt; 0 counts from end.
     * @param count    Return 'count' values at most. Range: &gt;= 0.
     * @param response Territory codes and information. Format: {@link com.mapcode.services.dto.TerritoriesDTO}.
     * @throws ApiException API exception, translated into HTTP status code.
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("territories")
    void getTerritories(
            @QueryParam(PARAM_OFFSET) @DefaultValue(DEFAULT_OFFSET) final int offset,
            @QueryParam(PARAM_COUNT) @DefaultValue(DEFAULT_COUNT) final int count,
            @Suspend(ApiConstants.SUSPEND_TIMEOUT) @Nonnull AsynchronousResponse response) throws ApiException;

    /**
     * Get info for a specific territory.
     *
     * @param paramTerritory Territory code. Range: any valid territory code, alpha or numeric.
     * @param paramContext   Context territory code for disambiguation. Range: any valid territory code, or alias.
     * @param response       Territory information. Format: {@link com.mapcode.services.dto.TerritoryDTO}.
     * @throws ApiException API exception, translated into HTTP status code.
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("territories/{" + PARAM_TERRITORY + '}')
    void getTerritory(
            @PathParam(PARAM_TERRITORY) @Nonnull final String paramTerritory,
            @QueryParam(PARAM_CONTEXT) @Nullable final String paramContext,
            @Suspend(ApiConstants.SUSPEND_TIMEOUT) @Nonnull AsynchronousResponse response) throws ApiException;
}
