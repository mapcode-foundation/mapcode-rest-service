/*
 * Copyright (C) 2016-2026, Stichting Mapcode Foundation (http://www.mapcode.com)
 */
package com.mapcode.services.implementation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * One polygon containing a lat/lon point, as returned by {@link BoundaryService}.
 * Immutable.
 */
public final class TerritoryMatch {

    @Nonnull private final String alphaCode;
    @Nullable private final String parentAlphaCode;
    private final int adminLevel;
    private final double area;

    public TerritoryMatch(
            @Nonnull final String alphaCode,
            @Nullable final String parentAlphaCode,
            final int adminLevel,
            final double area) {
        this.alphaCode = alphaCode;
        this.parentAlphaCode = parentAlphaCode;
        this.adminLevel = adminLevel;
        this.area = area;
    }

    @Nonnull
    public String getAlphaCode() {
        return alphaCode;
    }

    @Nullable
    public String getParentAlphaCode() {
        return parentAlphaCode;
    }

    public int getAdminLevel() {
        return adminLevel;
    }

    public double getArea() {
        return area;
    }
}
