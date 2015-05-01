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

@Path("/")
public interface RootResource {

    /**
     * This method provides help info.
     *
     * @return Returns help text as HTML.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    @Nonnull
    String getRoot();

    /**
     * This method provides help info.
     *
     * @return Returns help text as HTML.
     */
    @Path("help")
    @GET
    @Produces(MediaType.TEXT_HTML)
    @Nonnull
    String getHelp();

    /**
     * This method provides a version number.
     *
     * @param response Returns a version number as JSON.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("version")
    void getVersion(@Nonnull @Suspend(ApiConstants.SUSPEND_TIMEOUT) AsynchronousResponse response);

    /**
     * Get favicon.ico for help page.
     *
     * @return Returns a GIF icon.
     */
    @GET
    @Produces("image/gif")
    @Path("favicon.ico")
    @Nonnull
    byte[] getFavicon();
}
