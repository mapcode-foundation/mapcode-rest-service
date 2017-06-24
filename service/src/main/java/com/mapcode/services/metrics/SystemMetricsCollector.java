/*
 * Copyright (C) 2016-2017, Stichting Mapcode Foundation (http://www.mapcode.com)
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

/*
 * Copyright (C) 2012. TomTom International BV. All rights reserved.
 */

package com.mapcode.services.metrics;

import javax.annotation.Nullable;

/**
 * Interface through which metrics data can be provided. This may or may not be an actor.
 */
public interface SystemMetricsCollector {

    /**
     * Called whenever ANY mapcode to lat/lon request is made. The request may fail because
     * the parameters may be invalid though.
     *
     * @param client Client that issued the call. Can be null
     */
    public void addOneMapcodeToLatLonRequest(@Nullable final String client);

    /**
     * Called whenever a successful mapcode to lat/lon request is made.
     *
     * @param client Client that issued the call. Can be null
     */
    public void addOneValidMapcodeToLatLonRequest(@Nullable final String client);

    /**
     * Called whenever ANY lat/lon to mapcode request is made. The request may fail because
     * the parameters may be invalid though.
     *
     * @param client Client that issued the call. Can be null
     */
    public void addOneLatLonToMapcodeRequest(@Nullable final String client);

    /**
     * Called whenever a successful lat/lon to mapcode request is made.
     *
     * @param client Client that issued the call. Can be null
     */
    public void addOneValidLatLonToMapcodeRequest(@Nullable final String client);

    /**
     * Called whenever a alphabet request is made.
     *
     * @param client Client that issued the call. Can be null
     */
    public void addOneAlphabetRequest(@Nullable final String client);

    /**
     * Called whenever a territory request is made.
     *
     * @param client Client that issued the call. Can be null
     */
    public void addOneTerritoryRequest(@Nullable final String client);
}
