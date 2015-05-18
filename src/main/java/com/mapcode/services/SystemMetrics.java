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


import com.tomtom.speedtools.metrics.MultiMetricsData;

import javax.annotation.Nonnull;
import javax.management.MXBean;


/**
 * Mapcode metrics interface. Metrics are aggregated for multiple time intervals, being last minute, hour day, week and
 * month. For each time interval, the sum, average, standard deviation, minimum value and maximum value can be
 * retrieved.
 * <p/>
 * Note that the returned {@link MultiMetricsData} and embedded {@link com.tomtom.texas.utils.metrics.MetricsData}
 * instances are 'live' objects. They will continue to change when values are requested. They are thread-safe.
 */
@MXBean
public interface SystemMetrics {

    enum Metric {
        ALL_MAPCODE_TO_LATLON_REQUESTS,
        VALID_MAPCODE_TO_LATLON_REQUESTS,
        ALL_LATLON_TO_MAPCODE_REQUESTS,
        VALID_LATLON_TO_MAPCODE_REQUESTS,
        WARNINGS_AND_ERRORS
    }

    /**
     * @return Metric data for given metric.
     */
    @Nonnull
    MultiMetricsData getMetricData(@Nonnull Metric metric);
}
