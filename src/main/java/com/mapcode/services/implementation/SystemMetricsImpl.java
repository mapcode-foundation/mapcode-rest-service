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

package com.mapcode.services.implementation;

import com.mapcode.services.SystemMetrics;
import com.mapcode.services.SystemMetricsCollector;
import com.tomtom.speedtools.metrics.MultiMetricsCollector;
import com.tomtom.speedtools.metrics.MultiMetricsData;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.EnumMap;


/**
 * This class is purposely not an actor. It interacts with JMX and should therefore have as little messaging delay as
 * possible. It does, however, use the ActorSystem to provide it with a periodic timer event to store its data.
 */
public class SystemMetricsImpl implements SystemMetrics, SystemMetricsCollector {
    private final MultiMetricsCollector allMapcodeToLatLonRequests = MultiMetricsCollector.all();
    private final MultiMetricsCollector validMapcodeToLatLonRequests = MultiMetricsCollector.all();
    private final MultiMetricsCollector allLatLonToMapcodeRequests = MultiMetricsCollector.all();
    private final MultiMetricsCollector validLatLonToMapcodeRequests = MultiMetricsCollector.all();
    private final MultiMetricsCollector warningsAndErrors = MultiMetricsCollector.all();

    @Nonnull
    private final EnumMap<Metric, MultiMetricsCollector> all =
            new EnumMap<Metric, MultiMetricsCollector>(Metric.class) {{
                put(Metric.ALL_MAPCODE_TO_LATLON_REQUESTS, allMapcodeToLatLonRequests);
                put(Metric.VALID_MAPCODE_TO_LATLON_REQUESTS, validMapcodeToLatLonRequests);
                put(Metric.ALL_LATLON_TO_MAPCODE_REQUESTS, allLatLonToMapcodeRequests);
                put(Metric.VALID_LATLON_TO_MAPCODE_REQUESTS, validLatLonToMapcodeRequests);
                put(Metric.WARNINGS_AND_ERRORS, warningsAndErrors);
            }};

    @Inject
    public SystemMetricsImpl() {

        // Listen for log errors and warnings.
        Logger.getRootLogger().addAppender(new Log4jAppender());
    }

    /**
     * Appender used to count warnings and errors.
     */
    private class Log4jAppender extends AppenderSkeleton {
        @Override
        protected void append(@Nonnull final LoggingEvent loggingEvent) {
            if (loggingEvent.getLevel().equals(Level.WARN) ||
                    loggingEvent.getLevel().equals(Level.ERROR) ||
                    loggingEvent.getLevel().equals(Level.FATAL)) {
                // Increase counter first, then log - on the off chance that another errors occurs...
                warningsAndErrors.addValue(1);
            } else {
                // OK. Do NOT use LOG.debug (would be recursive).
            }
        }

        @Override
        public void close() {
            // Empty.
        }

        @Override
        public boolean requiresLayout() {
            return false;
        }
    }

    @Nonnull
    @Override
    public MultiMetricsData getMetricData(@Nonnull final Metric metric) {
        assert metric != null;
        return all.get(metric);
    }

    @Nonnull
    @Override
    public MultiMetricsData getTotalMapcodeToLatLonRequests() {
        return allMapcodeToLatLonRequests;
    }

    @Nonnull
    @Override
    public MultiMetricsData getValidMapcodeToLatLonRequests() {
        return validMapcodeToLatLonRequests;
    }

    @Nonnull
    @Override
    public MultiMetricsData getTotalLatLonToMapcodeRequests() {
        return allLatLonToMapcodeRequests;
    }

    @Nonnull
    @Override
    public MultiMetricsData getValidLatLonToMapcodeRequests() {
        return validLatLonToMapcodeRequests;
    }

    @Nonnull
    @Override
    public MultiMetricsData getWarningsAndErrors() {
        return warningsAndErrors;
    }

    @Override
    public void addOneMapcodeToLatLonRequest() {
        allMapcodeToLatLonRequests.addValue(1);
    }

    @Override
    public void addOneValidMapcodeToLatLonRequest() {
        validMapcodeToLatLonRequests.addValue(1);
    }

    @Override
    public void addOneLatLonToMapcodeRequest() {
        allLatLonToMapcodeRequests.addValue(1);
    }

    @Override
    public void addOneValidLatLonToMapcodeRequest() {
        validLatLonToMapcodeRequests.addValue(1);
    }
}
