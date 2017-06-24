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

package com.mapcode.services.metrics;


import com.tomtom.speedtools.metrics.MultiMetricsData;

import javax.annotation.Nonnull;
import javax.management.MXBean;


/**
 * Mapcode metrics interface. Metrics are aggregated for multiple time intervals, being last minute, hour day, week and
 * month. For each time interval, the sum, average, standard deviation, minimum value and maximum value can be
 * retrieved.
 *
 * Note that the returned {@link MultiMetricsData} and embedded {@link com.tomtom.speedtools.metrics.MetricsData}
 * instances are 'live' objects. They will continue to change when values are requested. They are thread-safe.
 */
@MXBean
public interface SystemMetrics {

    enum Metric {
        ALL_MAPCODE_TO_LATLON_REQUESTS,                 // All requests.
        ALL_CLIENT_NONE_MAPCODE_TO_LATLON_REQUESTS,     // Request per client type.
        ALL_CLIENT_IOS_MAPCODE_TO_LATLON_REQUESTS,
        ALL_CLIENT_ANDROID_MAPCODE_TO_LATLON_REQUESTS,
        ALL_CLIENT_WEB_MAPCODE_TO_LATLON_REQUESTS,
        VALID_MAPCODE_TO_LATLON_REQUESTS,

        ALL_LATLON_TO_MAPCODE_REQUESTS,                 // Requests.
        ALL_CLIENT_NONE_LATLON_TO_MAPCODE_REQUESTS,     // Requests per client type.
        ALL_CLIENT_IOS_LATLON_TO_MAPCODE_REQUESTS,
        ALL_CLIENT_ANDROID_LATLON_TO_MAPCODE_REQUESTS,
        ALL_CLIENT_WEB_LATLON_TO_MAPCODE_REQUESTS,
        VALID_LATLON_TO_MAPCODE_REQUESTS,

        ALL_ALPHABET_REQUESTS,
        ALL_TERRITORY_REQUESTS,
        WARNINGS_AND_ERRORS
    }

    enum Client {
        NONE,       // No client specified.
        IOS,        // iOS app.
        ANDROID,    // Android app.
        WEB         // Mapcode Foundation web page.
    }

    /**
     * Return metrics data.
     *
     * @param metric Metric to return data for.
     * @return Metric data for given metric.
     */
    @Nonnull
    MultiMetricsData getMetricData(@Nonnull Metric metric);

    /**
     * @return The total number of requests for mapcode to lat/lon.
     */
    @Nonnull
    MultiMetricsData getAllMapcodeToLatLonRequests();

    MultiMetricsData getAllClientNoneMapcodeToLatLonRequests();

    MultiMetricsData getAllClientIOSMapcodeToLatLonRequests();

    MultiMetricsData getAllClientAndroidMapcodeToLatLonRequests();

    MultiMetricsData getAllClientWebMapcodeToLatLonRequests();

    /**
     * @return The number of valid requests for mapcode to lat/lon.
     */
    @Nonnull
    MultiMetricsData getValidMapcodeToLatLonRequests();

    /**
     * @return The total number of requests for lat/lon to mapcode.
     */
    @Nonnull
    MultiMetricsData getAllLatLonToMapcodeRequests();

    MultiMetricsData getAllClientNoneLatLonToMapcodeRequests();

    MultiMetricsData getAllClientIOSLatLonToMapcodeRequests();

    MultiMetricsData getAllClientAndroidLatLonToMapcodeRequests();

    MultiMetricsData getAllClientWebLatLonToMapcodeRequests();

    /**
     * @return The number of valid requests for lat/lon to mapcode.
     */
    @Nonnull
    MultiMetricsData getValidLatLonToMapcodeRequests();

    /**
     * @return The total number of requests for alphabets.
     */
    @Nonnull
    MultiMetricsData getAllAlphabetRequests();

    /**
     * @return The total number of requests for territories.
     */
    @Nonnull
    MultiMetricsData getAllTerritoryRequests();

    /**
     * @return The number of warnings and errors that were logged through log4j.
     */
    @Nonnull
    MultiMetricsData getWarningsAndErrors();
}
