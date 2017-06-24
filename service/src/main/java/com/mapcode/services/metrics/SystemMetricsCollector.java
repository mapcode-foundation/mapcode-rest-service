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
