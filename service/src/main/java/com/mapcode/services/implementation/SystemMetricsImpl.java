/*
 * Copyright (C) 2016-2020, Stichting Mapcode Foundation (http://www.mapcode.com)
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

package com.mapcode.services.implementation;

import com.mapcode.services.metrics.SystemMetrics;
import com.mapcode.services.metrics.SystemMetricsCollector;
import com.tomtom.speedtools.metrics.MultiMetricsCollector;
import com.tomtom.speedtools.metrics.MultiMetricsData;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.EnumMap;


/**
 * This class is purposely not an actor. It interacts with JMX and should therefore have as little messaging delay as
 * possible. It does, however, use the ActorSystem to provide it with a periodic timer event to store its data.
 */
public class SystemMetricsImpl implements SystemMetrics, SystemMetricsCollector {
    private final MultiMetricsCollector allMapcodeToLatLonRequests = MultiMetricsCollector.all();
    private final MultiMetricsCollector allClientNoneMapcodeToLatLonRequests = MultiMetricsCollector.all();
    private final MultiMetricsCollector allClientIOSMapcodeToLatLonRequests = MultiMetricsCollector.all();
    private final MultiMetricsCollector allClientAndroidMapcodeToLatLonRequests = MultiMetricsCollector.all();
    private final MultiMetricsCollector allClientWebMapcodeToLatLonRequests = MultiMetricsCollector.all();
    private final MultiMetricsCollector validMapcodeToLatLonRequests = MultiMetricsCollector.all();

    private final MultiMetricsCollector allLatLonToMapcodeRequests = MultiMetricsCollector.all();
    private final MultiMetricsCollector allClientNoneLatLonToMapcodeRequests = MultiMetricsCollector.all();
    private final MultiMetricsCollector allClientIOSLatLonToMapcodeRequests = MultiMetricsCollector.all();
    private final MultiMetricsCollector allClientAndroidLatLonToMapcodeRequests = MultiMetricsCollector.all();
    private final MultiMetricsCollector allClientWebLatLonToMapcodeRequests = MultiMetricsCollector.all();
    private final MultiMetricsCollector validLatLonToMapcodeRequests = MultiMetricsCollector.all();

    private final MultiMetricsCollector allAlphabetRequests = MultiMetricsCollector.all();
    private final MultiMetricsCollector allTerritoryRequests = MultiMetricsCollector.all();
    private final MultiMetricsCollector warningsAndErrors = MultiMetricsCollector.all();

    @SuppressWarnings("ClassExtendsConcreteCollection")
    @Nonnull
    private final EnumMap<Metric, MultiMetricsCollector> all =
            new EnumMap<Metric, MultiMetricsCollector>(Metric.class) {{
                put(Metric.ALL_MAPCODE_TO_LATLON_REQUESTS, allMapcodeToLatLonRequests);
                put(Metric.ALL_CLIENT_NONE_MAPCODE_TO_LATLON_REQUESTS, allMapcodeToLatLonRequests);
                put(Metric.ALL_CLIENT_IOS_MAPCODE_TO_LATLON_REQUESTS, allMapcodeToLatLonRequests);
                put(Metric.ALL_CLIENT_ANDROID_MAPCODE_TO_LATLON_REQUESTS, allMapcodeToLatLonRequests);
                put(Metric.ALL_CLIENT_WEB_MAPCODE_TO_LATLON_REQUESTS, allMapcodeToLatLonRequests);
                put(Metric.VALID_MAPCODE_TO_LATLON_REQUESTS, validMapcodeToLatLonRequests);

                put(Metric.ALL_LATLON_TO_MAPCODE_REQUESTS, allLatLonToMapcodeRequests);
                put(Metric.ALL_CLIENT_NONE_LATLON_TO_MAPCODE_REQUESTS, allLatLonToMapcodeRequests);
                put(Metric.ALL_CLIENT_IOS_LATLON_TO_MAPCODE_REQUESTS, allLatLonToMapcodeRequests);
                put(Metric.ALL_CLIENT_ANDROID_LATLON_TO_MAPCODE_REQUESTS, allLatLonToMapcodeRequests);
                put(Metric.ALL_CLIENT_WEB_LATLON_TO_MAPCODE_REQUESTS, allLatLonToMapcodeRequests);
                put(Metric.VALID_LATLON_TO_MAPCODE_REQUESTS, validLatLonToMapcodeRequests);

                put(Metric.ALL_ALPHABET_REQUESTS, allAlphabetRequests);
                put(Metric.ALL_TERRITORY_REQUESTS, allTerritoryRequests);
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
    public MultiMetricsData getAllMapcodeToLatLonRequests() {
        return allMapcodeToLatLonRequests;
    }

    @Nonnull
    @Override
    public MultiMetricsData getValidMapcodeToLatLonRequests() {
        return validMapcodeToLatLonRequests;
    }

    @Nonnull
    @Override
    public MultiMetricsData getAllClientNoneMapcodeToLatLonRequests() {
        return allClientNoneMapcodeToLatLonRequests;
    }

    @Nonnull
    @Override
    public MultiMetricsData getAllClientIOSMapcodeToLatLonRequests() {
        return allClientIOSMapcodeToLatLonRequests;
    }

    @Nonnull
    @Override
    public MultiMetricsData getAllClientAndroidMapcodeToLatLonRequests() {
        return allClientAndroidMapcodeToLatLonRequests;
    }

    @Nonnull
    @Override
    public MultiMetricsData getAllClientWebMapcodeToLatLonRequests() {
        return allClientWebMapcodeToLatLonRequests;
    }

    @Nonnull
    @Override
    public MultiMetricsData getAllLatLonToMapcodeRequests() {
        return allLatLonToMapcodeRequests;
    }

    @Nonnull
    @Override
    public MultiMetricsData getValidLatLonToMapcodeRequests() {
        return validLatLonToMapcodeRequests;
    }

    @Nonnull
    @Override
    public MultiMetricsData getAllClientNoneLatLonToMapcodeRequests() {
        return allClientNoneLatLonToMapcodeRequests;
    }

    @Nonnull
    @Override
    public MultiMetricsData getAllClientIOSLatLonToMapcodeRequests() {
        return allClientIOSLatLonToMapcodeRequests;
    }

    @Nonnull
    @Override
    public MultiMetricsData getAllClientAndroidLatLonToMapcodeRequests() {
        return allClientAndroidLatLonToMapcodeRequests;
    }

    @Nonnull
    @Override
    public MultiMetricsData getAllClientWebLatLonToMapcodeRequests() {
        return allClientWebLatLonToMapcodeRequests;
    }

    @Nonnull
    @Override
    public MultiMetricsData getAllAlphabetRequests() {
        return allAlphabetRequests;
    }

    @Nonnull
    @Override
    public MultiMetricsData getAllTerritoryRequests() {
        return allTerritoryRequests;
    }

    @Nonnull
    @Override
    public MultiMetricsData getWarningsAndErrors() {
        return warningsAndErrors;
    }

    @Override
    public void addOneMapcodeToLatLonRequest(@Nullable final String client) {
        allMapcodeToLatLonRequests.addValue(1);
        switch (getClient(client)) {

            case NONE:
                allClientNoneMapcodeToLatLonRequests.addValue(1);
                break;

            case IOS:
                allClientIOSMapcodeToLatLonRequests.addValue(1);
                break;

            case ANDROID:
                allClientAndroidMapcodeToLatLonRequests.addValue(1);
                break;

            case WEB:
                allClientWebMapcodeToLatLonRequests.addValue(1);
                break;

            default:
                assert false;
                break;
        }
    }

    @Override
    public void addOneValidMapcodeToLatLonRequest(@Nullable final String client) {
        validMapcodeToLatLonRequests.addValue(1);
    }

    @Override
    public void addOneLatLonToMapcodeRequest(@Nullable final String client) {
        allLatLonToMapcodeRequests.addValue(1);
        switch (getClient(client)) {

            case NONE:
                allClientNoneLatLonToMapcodeRequests.addValue(1);
                break;

            case IOS:
                allClientIOSLatLonToMapcodeRequests.addValue(1);
                break;

            case ANDROID:
                allClientAndroidLatLonToMapcodeRequests.addValue(1);
                break;

            case WEB:
                allClientWebLatLonToMapcodeRequests.addValue(1);
                break;

            default:
                assert false;
                break;
        }
    }

    @Override
    public void addOneValidLatLonToMapcodeRequest(@Nullable final String client) {
        validLatLonToMapcodeRequests.addValue(1);
    }

    @Override
    public void addOneAlphabetRequest(@Nullable final String client) {
        allAlphabetRequests.addValue(1);
    }

    @Override
    public void addOneTerritoryRequest(@Nullable final String client) {
        allTerritoryRequests.addValue(1);
    }

    private static Client getClient(@Nullable final String client) {
        final Client id;
        if (client == null) {
            id = Client.NONE;
        } else if (client.equalsIgnoreCase(Client.IOS.toString())) {
            id = Client.IOS;
        } else if (client.equalsIgnoreCase(Client.ANDROID.toString())) {
            id = Client.ANDROID;
        } else if (client.equalsIgnoreCase(Client.WEB.toString())) {
            id = Client.WEB;
        } else {
            id = Client.NONE;
        }
        return id;
    }
}
