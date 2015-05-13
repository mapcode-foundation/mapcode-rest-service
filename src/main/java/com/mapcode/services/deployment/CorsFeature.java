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

package com.mapcode.services.deployment;

import org.jboss.resteasy.plugins.interceptors.CorsFilter;

import javax.annotation.Nonnull;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

/**
 * This class adds the Cross-Origin Resource Sharing feature to allow Javascrip to call
 * this service from another domain. It essentially add the calling domain to the list
 * of allowed origins in the HTTP header.
 *
 * The feature is enabled by adding it to web.xml
 *
 * <pre>
 *   &lt;context-param&gt;
 *     &lt;param-name>resteasy.providers&lt;/param-name>
 *     &lt;param-value>com.mapcode.services.deployment.CorsFeature&lt;/param-value>
 *   &lt;/context-param>
 * </pre>
 */
@Provider
public class CorsFeature implements Feature {

    @Override
    public boolean configure(@Nonnull final FeatureContext featureContext) {

        // Add the appropriate CORS header.
        final CorsFilter corsFilter = new CorsFilter();
        corsFilter.getAllowedOrigins().add("*");
        featureContext.register(corsFilter);
        return true;
    }
}
