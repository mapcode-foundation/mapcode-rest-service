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

import com.mapcode.services.dto.AlphabetListDTO;
import com.mapcode.services.dto.CoordinatesDTO;
import com.mapcode.services.dto.MapcodesDTO;
import com.mapcode.services.dto.TerritoryListDTO;
import com.tomtom.speedtools.apivalidation.exceptions.ApiException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
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
        TERRITORY,          // Force including the territory, even when the territory code is "AAA".
        ALPHABET            // Force including the mapcodeInAlphabet attribute, even if it is the same as the mapcode.
    }

    /**
     * Strings used as path or url parameters.
     */
    static final String PARAM_LAT_DEG = "lat";
    static final String PARAM_LON_DEG = "lon";
    static final String PARAM_PRECISION = "precision";
    static final String PARAM_TERRITORY = "territory";
    static final String PARAM_ALPHABET = "alphabet";
    static final String PARAM_CONTEXT = "context";
    static final String PARAM_TYPE = "type";
    static final String PARAM_CODE = "mapcode";
    static final String PARAM_INCLUDE = "include";
    static final String PARAM_COUNT = "count";
    static final String PARAM_OFFSET = "offset";
    static final String PARAM_DEBUG = "debug";

    static final String DEFAULT_OFFSET = "0";
    static final String DEFAULT_COUNT = "1000";

    // Unsupported operation.
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("codes")
    void convertLatLonToMapcode(
            @Suspended @Nonnull AsyncResponse response) throws ApiException;

    @GET
    @Produces(MediaType.APPLICATION_XML)
    @Path("xml/codes")
    void convertLatLonToMapcodeXml(
            @Suspended @Nonnull AsyncResponse response) throws ApiException;

    /**
     * Convert a lat/lon to one or more mapcodes. All possible mapcodes are returned.
     *
     * @param paramLatDeg    Latitude. Range: [-90, 90].
     * @param paramLonDeg    Longitude. Range: Any double, wrapped along the earth to [-180, 180].
     * @param paramPrecision Precision specifier; specifies additional mapcode digits. Range: [0, 2].
     * @param paramTerritory Specifies a territory context to create a local mapcode for. This is only useful for local mapcodes.
     *                       If the mapcode cannot be created for the territory, an exception is thrown.
     *                       Range: any valid territory code, alpha or numeric.
     * @param paramAlphabet  Alphabet. Range: any valid alphabet code, alpha or numeric.
     * @param paramInclude   Specifies whether to include the offset (in meters) from the mapcode center to the specified lat/lon.
     *                       Range: {@link ParamInclude}.
     * @param paramDebug     True for debugging purposes. Default is false.
     * @param response       One or more mapcodes. Format: {@link com.mapcode.services.dto.MapcodeDTO} for LOCAL and
     *                       INTERNATIONAL and {@link MapcodesDTO} for ALL.
     * @throws ApiException API exception, translated into HTTP status code.
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("codes/{" + PARAM_LAT_DEG + "},{" + PARAM_LON_DEG + '}')
    void convertLatLonToMapcode(
            @PathParam(PARAM_LAT_DEG) double paramLatDeg,
            @PathParam(PARAM_LON_DEG) double paramLonDeg,
            @QueryParam(PARAM_PRECISION) @DefaultValue("0") int paramPrecision,
            @QueryParam(PARAM_TERRITORY) @Nullable String paramTerritory,
            @QueryParam(PARAM_ALPHABET) @Nullable String paramAlphabet,
            @QueryParam(PARAM_INCLUDE) @DefaultValue("") @Nonnull String paramInclude,
            @QueryParam(PARAM_DEBUG) @DefaultValue("false") @Nonnull String paramDebug,
            @Suspended @Nonnull AsyncResponse response) throws ApiException;

    @GET
    @Produces(MediaType.APPLICATION_XML)
    @Path("xml/codes/{" + PARAM_LAT_DEG + "},{" + PARAM_LON_DEG + '}')
    void convertLatLonToMapcodeXml(
            @PathParam(PARAM_LAT_DEG) double paramLatDeg,
            @PathParam(PARAM_LON_DEG) double paramLonDeg,
            @QueryParam(PARAM_PRECISION) @DefaultValue("0") int paramPrecision,
            @QueryParam(PARAM_TERRITORY) @Nullable String paramTerritory,
            @QueryParam(PARAM_ALPHABET) @Nullable String paramAlphabet,
            @QueryParam(PARAM_INCLUDE) @DefaultValue("") @Nonnull String paramInclude,
            @QueryParam(PARAM_DEBUG) @DefaultValue("false") @Nonnull String paramDebug,
            @Suspended @Nonnull AsyncResponse response) throws ApiException;

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
     * @param paramAlphabet  Alphabet. Range: any valid alphabet code, alpha or numeric.
     * @param paramInclude   Specifies whether to include the offset (in meters) from the mapcode center to the specified lat/lon.
     *                       Range: {@link ParamInclude}.
     * @param paramDebug     True for debugging purposes. Default is false.
     * @param response       One or more mapcodes. Format: {@link com.mapcode.services.dto.MapcodeDTO} for LOCAL and
     *                       INTERNATIONAL and {@link MapcodesDTO} for ALL.
     * @throws ApiException API exception, translated into HTTP status code.
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("codes/{" + PARAM_LAT_DEG + "},{" + PARAM_LON_DEG + "}/{" + PARAM_TYPE + '}')
    void convertLatLonToMapcode(
            @PathParam(PARAM_LAT_DEG) double paramLatDeg,
            @PathParam(PARAM_LON_DEG) double paramLonDeg,
            @PathParam(PARAM_TYPE) @Nullable String paramType,
            @QueryParam(PARAM_PRECISION) @DefaultValue("0") int paramPrecision,
            @QueryParam(PARAM_TERRITORY) @Nullable String paramTerritory,
            @QueryParam(PARAM_ALPHABET) @Nullable String paramAlphabet,
            @QueryParam(PARAM_INCLUDE) @DefaultValue("") @Nonnull String paramInclude,
            @QueryParam(PARAM_DEBUG) @DefaultValue("false") @Nonnull String paramDebug,
            @Suspended @Nonnull AsyncResponse response) throws ApiException;

    @GET
    @Produces(MediaType.APPLICATION_XML)
    @Path("xml/codes/{" + PARAM_LAT_DEG + "},{" + PARAM_LON_DEG + "}/{" + PARAM_TYPE + '}')
    void convertLatLonToMapcodeXml(
            @PathParam(PARAM_LAT_DEG) double paramLatDeg,
            @PathParam(PARAM_LON_DEG) double paramLonDeg,
            @PathParam(PARAM_TYPE) @Nullable String paramType,
            @QueryParam(PARAM_PRECISION) @DefaultValue("0") int paramPrecision,
            @QueryParam(PARAM_TERRITORY) @Nullable String paramTerritory,
            @QueryParam(PARAM_ALPHABET) @Nullable String paramAlphabet,
            @QueryParam(PARAM_INCLUDE) @DefaultValue("") @Nonnull String paramInclude,
            @QueryParam(PARAM_DEBUG) @DefaultValue("false") @Nonnull String paramDebug,
            @Suspended @Nonnull AsyncResponse response) throws ApiException;

    // Unsupported operation.
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("coords")
    void convertMapcodeToLatLon(
            @Suspended @Nonnull AsyncResponse response) throws ApiException;

    @GET
    @Produces(MediaType.APPLICATION_XML)
    @Path("xml/coords")
    void convertMapcodeToLatLonXml(
            @Suspended @Nonnull AsyncResponse response) throws ApiException;

    /**
     * Convert a mapcode into a lat/lon pair.
     *
     * @param paramCode    Mapcode to convert.
     * @param paramContext Specifies a territory context for interpretation of the mapcode.
     *                     Range: any valid territory.
     * @param paramDebug   True for debugging purposes. Default is false.
     * @param response     Lat/lon. Format: {@link CoordinatesDTO}.
     * @throws ApiException API exception, translated into HTTP status code.
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("coords/{" + PARAM_CODE + '}')
    void convertMapcodeToLatLon(
            @PathParam(PARAM_CODE) @Nonnull String paramCode,
            @QueryParam(PARAM_CONTEXT) @Nullable String paramContext,
            @QueryParam(PARAM_DEBUG) @DefaultValue("false") @Nonnull String paramDebug,
            @Suspended @Nonnull AsyncResponse response) throws ApiException;

    @GET
    @Produces(MediaType.APPLICATION_XML)
    @Path("xml/coords/{" + PARAM_CODE + '}')
    void convertMapcodeToLatLonXml(
            @PathParam(PARAM_CODE) @Nonnull String paramCode,
            @QueryParam(PARAM_CONTEXT) @Nullable String paramContext,
            @QueryParam(PARAM_DEBUG) @DefaultValue("false") @Nonnull String paramDebug,
            @Suspended @Nonnull AsyncResponse response) throws ApiException;

    /**
     * Get a list of all valid territory codes.
     *
     * @param offset     Return values from 'offset'. Range: &gt;= 0 counts from start, &lt; 0 counts from end.
     * @param count      Return 'count' values at most. Range: &gt;= 0.
     * @param paramDebug True for debugging purposes. Default is false.
     * @param response   Territory codes and information. Format: {@link TerritoryListDTO}.
     * @throws ApiException API exception, translated into HTTP status code.
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("territories")
    void getTerritories(
            @QueryParam(PARAM_OFFSET) @DefaultValue(DEFAULT_OFFSET) int offset,
            @QueryParam(PARAM_COUNT) @DefaultValue(DEFAULT_COUNT) int count,
            @QueryParam(PARAM_DEBUG) @DefaultValue("false") @Nonnull String paramDebug,
            @Suspended @Nonnull AsyncResponse response) throws ApiException;

    @GET
    @Produces(MediaType.APPLICATION_XML)
    @Path("xml/territories")
    void getTerritoriesXml(
            @QueryParam(PARAM_OFFSET) @DefaultValue(DEFAULT_OFFSET) int offset,
            @QueryParam(PARAM_COUNT) @DefaultValue(DEFAULT_COUNT) int count,
            @QueryParam(PARAM_DEBUG) @DefaultValue("false") @Nonnull String paramDebug,
            @Suspended @Nonnull AsyncResponse response) throws ApiException;

    /**
     * Get info for a specific territory.
     *
     * @param paramTerritory Territory code. Range: any valid territory code, alpha or numeric.
     * @param paramContext   Context territory code for disambiguation. Range: any valid territory code, or alias.
     * @param paramDebug     True for debugging purposes. Default is false.
     * @param response       Territory information. Format: {@link com.mapcode.services.dto.TerritoryDTO}.
     * @throws ApiException API exception, translated into HTTP status code.
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("territories/{" + PARAM_TERRITORY + '}')
    void getTerritory(
            @PathParam(PARAM_TERRITORY) @Nonnull String paramTerritory,
            @QueryParam(PARAM_CONTEXT) @Nullable String paramContext,
            @QueryParam(PARAM_DEBUG) @DefaultValue("false") @Nonnull String paramDebug,
            @Suspended @Nonnull AsyncResponse response) throws ApiException;

    @GET
    @Produces(MediaType.APPLICATION_XML)
    @Path("xml/territories/{" + PARAM_TERRITORY + '}')
    void getTerritoryXml(
            @PathParam(PARAM_TERRITORY) @Nonnull String paramTerritory,
            @QueryParam(PARAM_CONTEXT) @Nullable String paramContext,
            @QueryParam(PARAM_DEBUG) @DefaultValue("false") @Nonnull String paramDebug,
            @Suspended @Nonnull AsyncResponse response) throws ApiException;

    /**
     * Get a list of all valid alphabet codes.
     *
     * @param offset     Return values from 'offset'. Range: &gt;= 0 counts from start, &lt; 0 counts from end.
     * @param count      Return 'count' values at most. Range: &gt;= 0.
     * @param paramDebug True for debugging purposes. Default is false.
     * @param response   Alphabet codes and information. Format: {@link AlphabetListDTO}.
     * @throws ApiException API exception, translated into HTTP status code.
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("alphabets")
    void getAlphabets(
            @QueryParam(PARAM_OFFSET) @DefaultValue(DEFAULT_OFFSET) int offset,
            @QueryParam(PARAM_COUNT) @DefaultValue(DEFAULT_COUNT) int count,
            @QueryParam(PARAM_DEBUG) @DefaultValue("false") @Nonnull String paramDebug,
            @Suspended @Nonnull AsyncResponse response) throws ApiException;

    @GET
    @Produces(MediaType.APPLICATION_XML)
    @Path("xml/alphabets")
    void getAlphabetsXml(
            @QueryParam(PARAM_OFFSET) @DefaultValue(DEFAULT_OFFSET) int offset,
            @QueryParam(PARAM_COUNT) @DefaultValue(DEFAULT_COUNT) int count,
            @QueryParam(PARAM_DEBUG) @DefaultValue("false") @Nonnull String paramDebug,
            @Suspended @Nonnull AsyncResponse response) throws ApiException;

    /**
     * Get info for a specific alphabet.
     *
     * @param paramAlphabet Alphabet code. Range: any valid alphabet code, alpha or numeric.
     * @param paramDebug    True for debugging purposes. Default is false.
     * @param response      Territory information. Format: {@link com.mapcode.services.dto.AlphabetDTO}.
     * @throws ApiException API exception, translated into HTTP status code.
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("alphabets/{" + PARAM_ALPHABET + '}')
    void getAlphabet(
            @PathParam(PARAM_ALPHABET) @Nonnull String paramAlphabet,
            @QueryParam(PARAM_DEBUG) @DefaultValue("false") @Nonnull String paramDebug,
            @Suspended @Nonnull AsyncResponse response) throws ApiException;

    @GET
    @Produces(MediaType.APPLICATION_XML)
    @Path("xml/alphabets/{" + PARAM_ALPHABET + '}')
    void getAlphabetXml(
            @PathParam(PARAM_ALPHABET) @Nonnull String paramAlphabet,
            @QueryParam(PARAM_DEBUG) @DefaultValue("false") @Nonnull String paramDebug,
            @Suspended @Nonnull AsyncResponse response) throws ApiException;
}
