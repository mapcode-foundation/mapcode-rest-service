/*
 * Copyright (C) 2016-2018, Stichting Mapcode Foundation (http://www.mapcode.com)
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

package com.mapcode.services;

import com.tomtom.speedtools.apivalidation.exceptions.ApiException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;

import static com.mapcode.services.MapcodeResource.*;

/**
 * This class handle the Mapcode REST API, which includes conversions to and from mapcodes.
 */
@Path("/mapcode/xml")
public interface OnlyXmlResource {

    @Path("version")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    void getVersionXml(@Suspended @Nonnull AsyncResponse response);

    @Path("status")
    @Produces(MediaType.APPLICATION_XML)
    @GET
    void getStatusXml(@Suspended @Nonnull AsyncResponse response);

    @GET
    @Produces(MediaType.APPLICATION_XML)
    @Path("codes")
    void convertLatLonToMapcodeXml(
            @Suspended @Nonnull AsyncResponse response) throws ApiException;

    @GET
    @Produces(MediaType.APPLICATION_XML)
    @Path("codes/{" + PARAM_LAT_DEG + "},{" + PARAM_LON_DEG + '}')
    void convertLatLonToMapcodeXml(
            @PathParam(PARAM_LAT_DEG) double paramLatDeg,
            @PathParam(PARAM_LON_DEG) double paramLonDeg,
            @QueryParam(PARAM_PRECISION) @DefaultValue("0") int paramPrecision,
            @QueryParam(PARAM_TERRITORY) @Nullable String paramTerritory,
            @QueryParam(PARAM_CONTEXT) @Nullable String paramContextMustBeNull,
            @QueryParam(PARAM_ALPHABET) @Nullable String paramAlphabet,
            @QueryParam(PARAM_INCLUDE) @DefaultValue("") @Nonnull String paramInclude,
            @QueryParam(PARAM_CLIENT) @DefaultValue("") @Nonnull String paramClient,
            @QueryParam(PARAM_ALLOW_LOG) @DefaultValue("true") @Nonnull String paramAllowLog,
            @Suspended @Nonnull AsyncResponse response) throws ApiException;

    @GET
    @Produces(MediaType.APPLICATION_XML)
    @Path("codes/{" + PARAM_LAT_DEG + "},{" + PARAM_LON_DEG + "}/{" + PARAM_TYPE + '}')
    void convertLatLonToMapcodeXml(
            @PathParam(PARAM_LAT_DEG) double paramLatDeg,
            @PathParam(PARAM_LON_DEG) double paramLonDeg,
            @PathParam(PARAM_TYPE) @Nullable String paramType,
            @QueryParam(PARAM_PRECISION) @DefaultValue("0") int paramPrecision,
            @QueryParam(PARAM_TERRITORY) @Nullable String paramTerritory,
            @QueryParam(PARAM_CONTEXT) @Nullable String paramContextMustBeNull,
            @QueryParam(PARAM_ALPHABET) @Nullable String paramAlphabet,
            @QueryParam(PARAM_INCLUDE) @DefaultValue("") @Nonnull String paramInclude,
            @QueryParam(PARAM_CLIENT) @DefaultValue("") @Nonnull String paramClient,
            @QueryParam(PARAM_ALLOW_LOG) @DefaultValue("true") @Nonnull String paramDebug,
            @Suspended @Nonnull AsyncResponse response) throws ApiException;

    @GET
    @Produces(MediaType.APPLICATION_XML)
    @Path("coords")
    void convertMapcodeToLatLonXml(
            @Suspended @Nonnull AsyncResponse response) throws ApiException;

    @GET
    @Produces(MediaType.APPLICATION_XML)
    @Path("coords/{" + PARAM_MAPCODE + '}')
    void convertMapcodeToLatLonXml(
            @PathParam(PARAM_MAPCODE) @Nonnull String paramCode,
            @QueryParam(PARAM_CONTEXT) @Nullable String paramContext,
            @QueryParam(PARAM_TERRITORY) @Nullable String paramTerritoryMustBeNull,
            @QueryParam(PARAM_INCLUDE) @DefaultValue("") @Nonnull String paramInclude,
            @QueryParam(PARAM_CLIENT) @DefaultValue("") @Nonnull String paramClient,
            @QueryParam(PARAM_ALLOW_LOG) @DefaultValue("true") @Nonnull String paramDebug,
            @Suspended @Nonnull AsyncResponse response) throws ApiException;

    @GET
    @Produces(MediaType.APPLICATION_XML)
    @Path("territories")
    void getTerritoriesXml(
            @QueryParam(PARAM_OFFSET) @DefaultValue(DEFAULT_OFFSET) int offset,
            @QueryParam(PARAM_COUNT) @DefaultValue(DEFAULT_COUNT) int count,
            @QueryParam(PARAM_CLIENT) @DefaultValue("") @Nonnull String paramClient,
            @QueryParam(PARAM_ALLOW_LOG) @DefaultValue("true") @Nonnull String paramAllowLog,
            @Suspended @Nonnull AsyncResponse response) throws ApiException;

    @GET
    @Produces(MediaType.APPLICATION_XML)
    @Path("territories/{" + PARAM_TERRITORY + '}')
    void getTerritoryXml(
            @PathParam(PARAM_TERRITORY) @Nonnull String paramTerritory,
            @QueryParam(PARAM_CONTEXT) @Nullable String paramContext,
            @QueryParam(PARAM_CLIENT) @DefaultValue("") @Nonnull String paramClient,
            @QueryParam(PARAM_ALLOW_LOG) @DefaultValue("true") @Nonnull String paramAllowLog,
            @Suspended @Nonnull AsyncResponse response) throws ApiException;

    @GET
    @Produces(MediaType.APPLICATION_XML)
    @Path("alphabets")
    void getAlphabetsXml(
            @QueryParam(PARAM_OFFSET) @DefaultValue(DEFAULT_OFFSET) int offset,
            @QueryParam(PARAM_COUNT) @DefaultValue(DEFAULT_COUNT) int count,
            @QueryParam(PARAM_CLIENT) @DefaultValue("") @Nonnull String paramClient,
            @QueryParam(PARAM_ALLOW_LOG) @DefaultValue("true") @Nonnull String paramAllowLog,
            @Suspended @Nonnull AsyncResponse response) throws ApiException;

    @GET
    @Produces(MediaType.APPLICATION_XML)
    @Path("alphabets/{" + PARAM_ALPHABET + '}')
    void getAlphabetXml(
            @PathParam(PARAM_ALPHABET) @Nonnull String paramAlphabet,
            @QueryParam(PARAM_CLIENT) @DefaultValue("") @Nonnull String paramClient,
            @QueryParam(PARAM_ALLOW_LOG) @DefaultValue("true") @Nonnull String paramAllowLog,
            @Suspended @Nonnull AsyncResponse response) throws ApiException;
}
