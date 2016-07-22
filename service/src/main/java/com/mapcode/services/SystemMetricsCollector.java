/*
 * Copyright (C) 2012. TomTom International BV. All rights reserved.
 */

package com.mapcode.services;

/**
 * Interface through which metrics data can be provided. This may or may not be an actor.
 */
public interface SystemMetricsCollector {

    /**
     * Called whenever ANY mapcode to lat/lon request is made. The request may fail because
     * the parameters may be invalid though.
     */
    public void addOneMapcodeToLatLonRequest();

    /**
     * Called whenever a successful mapcode to lat/lon request is made.
     */
    public void addOneValidMapcodeToLatLonRequest();

    /**
     * Called whenever ANY lat/lon to mapcode request is made. The request may fail because
     * the parameters may be invalid though.
     */
    public void addOneLatLonToMapcodeRequest();

    /**
     * Called whenever a successful lat/lon to mapcode request is made.
     */
    public void addOneValidLatLonToMapcodeRequest();

    /**
     * Called whenever a alphabet request is made.
     */
    public void addOneAlphabetRequest();

    /**
     * Called whenever a territory request is made.
     */
    public void addOneTerritoryRequest();
}
