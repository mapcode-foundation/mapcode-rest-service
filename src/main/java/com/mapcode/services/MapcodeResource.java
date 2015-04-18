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
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/mapcode")
public interface MapcodeResource {

    public static final String PARAM_API_KEY = "apiKey";
    public static final String PARAM_LAT = "latDeg";
    public static final String PARAM_LON = "lonDeg";
    public static final String PARAM_PRECISION = "precision";


    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path(PARAM_API_KEY + "/from" + PARAM_LAT + '/' + PARAM_LON + '/' + PARAM_PRECISION)
    void getMapcodeFrom(
            @Nonnull @PathParam(PARAM_API_KEY) final String apiKey,
            @Nonnull @PathParam(PARAM_LAT) final String latDeg,
            @Nonnull @PathParam(PARAM_LON) final String lonDeg,
            @Nonnull @PathParam(PARAM_PRECISION) final String precision,
            @Nonnull @Suspend(ApiConstants.SUSPEND_TIMEOUT) AsynchronousResponse response) throws ApiException;
}
