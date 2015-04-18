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

    public static final String PARAM_LAT_DEG = "lat";
    public static final String PARAM_LON_DEG = "lon";
    public static final String PARAM_PRECISION = "precision";
    public static final String PARAM_TERRITORY = "territory";
    public static final String PARAM_TYPE = "type";
    public static final String PARAM_MAPCODE = "mapcode";

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("to/{" + PARAM_LAT_DEG + "}/{" + PARAM_LON_DEG + '}')
    void convertLatLonToMapcode(
            @Nonnull @PathParam(PARAM_LAT_DEG) final String latDeg,
            @Nonnull @PathParam(PARAM_LON_DEG) final String lonDeg,
            @Nonnull @QueryParam(PARAM_PRECISION) @DefaultValue("0") final String precision,
            @Nullable @QueryParam(PARAM_TERRITORY) final String territory,
            @Nonnull @QueryParam(PARAM_TYPE) @DefaultValue("shortest") final String type,
            @Nonnull @Suspend(ApiConstants.SUSPEND_TIMEOUT) AsynchronousResponse response) throws ApiException;

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("from/{" + PARAM_MAPCODE + '}')
    void convertMapcodeToLatLon(
            @Nonnull @PathParam(PARAM_MAPCODE) final String mapcode,
            @Nullable @QueryParam(PARAM_TERRITORY) final String territory,
            @Nonnull @Suspend(ApiConstants.SUSPEND_TIMEOUT) AsynchronousResponse response) throws ApiException;

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("territory")
    void getTerritories(@Nonnull @Suspend(ApiConstants.SUSPEND_TIMEOUT) AsynchronousResponse response) throws ApiException;

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("territory/{" + PARAM_TERRITORY + '}')
    void getTerritory(
            @Nonnull @PathParam(PARAM_TERRITORY) final String territory,
            @Nonnull @Suspend(ApiConstants.SUSPEND_TIMEOUT) AsynchronousResponse response) throws ApiException;
}
