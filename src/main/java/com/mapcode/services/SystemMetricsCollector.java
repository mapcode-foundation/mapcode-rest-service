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
    public void allMapcodeToLatLonRequests();

    /**
     * Called whenever a successful mapcode to lat/lon request is made.
     */
    public void validMapcodeToLatLonRequests();

    /**
     * Called whenever ANY lat/lon to mapcode request is made. The request may fail because
     * the parameters may be invalid though.
     */
    public void allLatLonToMapcodeRequests();

    /**
     * Called whenever a successful lat/lon to mapcode request is made.
     */
    public void validLatLonToMapcodeRequests();
}
