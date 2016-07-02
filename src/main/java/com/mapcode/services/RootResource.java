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

import org.jboss.resteasy.annotations.Suspend;
import org.jboss.resteasy.spi.AsynchronousResponse;

import javax.annotation.Nonnull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/mapcode")
public interface RootResource {

    /**
     * This method provides help info.
     *
     * @return Returns help text as HTML.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    @Nonnull
    String getHelpHTML();

    /**
     * This method provides a version number for the service. Normally, this is the
     * version number of the pom.xml file.
     *
     * @param response Returns a version number as JSON.
     */
    @Path("version")
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    void getVersion(@Nonnull @Suspend(ApiConstants.SUSPEND_TIMEOUT) AsynchronousResponse response);

    /**
     * This method returns whether the service is operational or not (status code 200 is OK).
     *
     * @param response Returns a version number as JSON.
     */
    @Path("status")
    @GET
    void getStatus(@Nonnull @Suspend(ApiConstants.SUSPEND_TIMEOUT) AsynchronousResponse response);

    /**
     * This method returns system metrics.
     *
     * @param response Returns a system metrics.
     */
    @Path("metrics")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    void getMetrics(@Nonnull @Suspend(ApiConstants.SUSPEND_TIMEOUT) AsynchronousResponse response);
}
