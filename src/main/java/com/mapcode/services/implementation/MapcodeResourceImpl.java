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

package com.mapcode.services.implementation;

import akka.dispatch.Futures;
import com.mapcode.*;
import com.mapcode.Territory.NameFormat;
import com.mapcode.services.ApiConstants;
import com.mapcode.services.MapcodeResource;
import com.mapcode.services.binders.*;
import com.tomtom.speedtools.apivalidation.ApiDataBinder;
import com.tomtom.speedtools.apivalidation.exceptions.ApiException;
import com.tomtom.speedtools.apivalidation.exceptions.ApiInvalidFormatException;
import com.tomtom.speedtools.apivalidation.exceptions.ApiNotFoundException;
import com.tomtom.speedtools.rest.ResourceProcessor;
import com.tomtom.speedtools.utils.MathUtils;
import org.jboss.resteasy.spi.AsynchronousResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * This class implements the REST API that deals with TTBin files.
 */
public class MapcodeResourceImpl implements MapcodeResource {
    private static final Logger LOG = LoggerFactory.getLogger(MapcodeResourceImpl.class);

    private enum FilterType {
        ALL,
        SHORTEST
    }

    @Nonnull
    private final ResourceProcessor processor;

    @Nonnull
    private final String TERRITORIES;

    @Nonnull
    private final String TYPES;

    @Inject
    public MapcodeResourceImpl(@Nonnull final ResourceProcessor processor) {
        assert processor != null;
        this.processor = processor;

        final StringBuilder sb1 = new StringBuilder();
        for (final Territory territory : Territory.values()) {
            sb1.append(territory.toNameFormat(NameFormat.MINIMAL_UNAMBIGUOUS));
            sb1.append('=');
            sb1.append(territory.getTerritoryCode());
            sb1.append('|');
        }
        TERRITORIES = sb1.toString();

        final StringBuilder sb2 = new StringBuilder();
        for (final FilterType filterType : FilterType.values()) {
            sb2.append(filterType.toString().toLowerCase());
            sb2.append('|');
        }
        TYPES = sb2.toString();
    }

    @Override
    public void convertLatLonToMapcode(
            @Nonnull final String latDeg,
            @Nonnull final String lonDeg,
            @Nonnull final String precision,
            @Nullable final String territory,
            @Nonnull final String type,
            @Nonnull final AsynchronousResponse response) throws ApiException {
        assert latDeg != null;
        assert lonDeg != null;
        assert precision != null;
        assert response != null;

        processor.process("convertLatLonToMapcode", LOG, response, () -> {
            LOG.debug("convertLatLonToMapcode: lat={}, lon={}, precision={}, territory={}", latDeg, lonDeg, precision, territory);

            final double latValue;
            try {
                latValue = Double.valueOf(latDeg);
                if (!MathUtils.isBetween(latValue, ApiConstants.API_LAT_MIN, ApiConstants.API_LAT_MAX)) {
                    throw new NumberFormatException();
                }
            } catch (final NumberFormatException ignored) {
                throw new ApiInvalidFormatException("lat", latDeg, "[" + ApiConstants.API_LAT_MIN + ", " + ApiConstants.API_LAT_MAX + ']');
            }

            final double lonValue;
            try {
                lonValue = Double.valueOf(lonDeg);
                if (!MathUtils.isBetween(lonValue, ApiConstants.API_LON_MIN, ApiConstants.API_LON_MAX)) {
                    throw new NumberFormatException();
                }
            } catch (final NumberFormatException ignored) {
                throw new ApiInvalidFormatException("lon", lonDeg, "[" + ApiConstants.API_LON_MIN + ", " + ApiConstants.API_LON_MAX + ']');
            }

            final int precisionValue;
            try {
                precisionValue = Integer.valueOf(precision);
                if (!MathUtils.isBetween(precisionValue, 0, 2)) {
                    throw new NumberFormatException();
                }
            } catch (final NumberFormatException ignored) {
                throw new ApiInvalidFormatException("precision", precision, "[0, 2]");
            }

            Territory territoryValue = null;
            if (territory != null) {
                try {
                    final int territoryCode = Integer.valueOf(territory);
                    territoryValue = Territory.fromTerritoryCode(territoryCode);
                } catch (final IllegalArgumentException ignored) {
                    try {
                        territoryValue = Territory.fromString(territory);
                    } catch (final UnknownTerritoryException ignored2) {
                        throw new ApiInvalidFormatException("territory", territory, TERRITORIES);
                    }
                }
            }
            assert ((territory == null) && (territoryValue == null)) || ((territory != null) && (territoryValue != null));

            FilterType typeValue;
            try {
                typeValue = FilterType.valueOf(type.toUpperCase());
            } catch (final IllegalArgumentException ignored) {
                throw new ApiInvalidFormatException("type", type, TYPES);
            }

            ApiDataBinder binder;
            switch (typeValue) {
                case ALL:
                    final List<MapcodeBinder> list = new ArrayList<>();
                    final List<Mapcode> mapcodes;
                    if (territory == null) {
                        mapcodes = MapcodeCodec.encode(latValue, lonValue);
                    } else {
                        mapcodes = MapcodeCodec.encode(latValue, lonValue, territoryValue);
                    }
                    for (final Mapcode mapcode : mapcodes) {
                        final MapcodeBinder item = new MapcodeBinder(getMapcodePrecision(mapcode, precisionValue), mapcode.getTerritory());
                        list.add(item);
                    }
                    final MapcodesBinder mapcodesBinder = new MapcodesBinder(list);
                    mapcodesBinder.validate();
                    binder = mapcodesBinder;
                    break;

                case SHORTEST:
                    final Mapcode mapcode = MapcodeCodec.encodeToShortest(latValue, lonValue);
                    final MapcodeBinder mapcodeBinder = new MapcodeBinder(getMapcodePrecision(mapcode, precisionValue), mapcode.getTerritory());
                    mapcodeBinder.validate();
                    binder = mapcodeBinder;
                    break;

                default:
                    assert false;
                    binder = null;
            }
            response.setResponse(Response.ok(binder).build());

            // The response is already set within this method body.
            return Futures.successful(null);
        });
    }

    @Override
    public void convertMapcodeToLatLon(
            @Nonnull final String mapcode,
            @Nullable final String territory,
            @Nonnull final AsynchronousResponse response) throws ApiException {
        assert mapcode != null;
        assert response != null;

        processor.process("convertMapcodeToLatLon", LOG, response, () -> {
            LOG.debug("convertMapcodeToLatLon: mapcode={}, territory={}", mapcode, territory);

            Territory territoryValue = null;
            if (territory != null) {
                try {
                    final int territoryCode = Integer.valueOf(territory);
                    territoryValue = Territory.fromTerritoryCode(territoryCode);
                } catch (final IllegalArgumentException ignored) {
                    try {
                        territoryValue = Territory.fromString(territory);
                    } catch (final UnknownTerritoryException ignored2) {
                        throw new ApiInvalidFormatException("territory", territory, TERRITORIES);
                    }
                }
            }
            assert ((territory == null) && (territoryValue == null)) || ((territory != null) && (territoryValue != null));

            try {
                final Point point;
                if (territory == null) {
                    point = MapcodeCodec.decode(mapcode);
                } else {
                    point = MapcodeCodec.decode(mapcode, territoryValue);
                }
                final PointBinder binder = new PointBinder(point.getLatDeg(), point.getLonDeg());
                binder.validate();
                response.setResponse(Response.ok(binder).build());
            } catch (final UnknownMapcodeException e) {
                throw new ApiNotFoundException(e.getMessage());
            }

            // The response is already set within this method body.
            return Futures.successful(null);
        });
    }

    @Override
    public void getTerritories(@Nonnull final AsynchronousResponse response) throws ApiException {
        assert response != null;

        processor.process("getTerritories", LOG, response, () -> {

            final List<TerritoryBinder> binders = new ArrayList<>();
            for (final Territory territory : Territory.values()) {
                final Territory parentTerritory = territory.getParentTerritory();
                final TerritoryBinder binder = new TerritoryBinder(
                        territory.getTerritoryCode(),
                        territory.getFullName(),
                        (parentTerritory == null) ? null : parentTerritory.toNameFormat(NameFormat.MINIMAL_UNAMBIGUOUS),
                        territory.getAliases(),
                        territory.getFullNameAliases()
                );
                binders.add(binder);
            }
            final TerritoriesBinder binder = new TerritoriesBinder(binders);
            binder.validate();
            response.setResponse(Response.ok(binder).build());

            // The response is already set within this method body.
            return Futures.successful(null);
        });
    }

    @Override
    public void getTerritory(
            @Nullable final String territory,
            @Nonnull final AsynchronousResponse response) throws ApiException {
        assert territory != null;
        assert response != null;

        processor.process("getTerritory", LOG, response, () -> {
            LOG.debug("getTerritory: territory={}", territory);

            Territory territoryValue = null;
            try {
                final int territoryCode = Integer.valueOf(territory);
                territoryValue = Territory.fromTerritoryCode(territoryCode);
            } catch (final IllegalArgumentException ignored) {
                try {
                    territoryValue = Territory.fromString(territory);
                } catch (final UnknownTerritoryException ignored2) {
                    throw new ApiInvalidFormatException("territory", territory, TERRITORIES);
                }
            }

            final Territory parentTerritory = territoryValue.getParentTerritory();
            final TerritoryBinder binder = new TerritoryBinder(
                    territoryValue.getTerritoryCode(),
                    territoryValue.getFullName(),
                    (parentTerritory == null) ? null : parentTerritory.toNameFormat(NameFormat.MINIMAL_UNAMBIGUOUS),
                    territoryValue.getAliases(),
                    territoryValue.getFullNameAliases()
            );
            binder.validate();
            response.setResponse(Response.ok(binder).build());

            // The response is already set within this method body.
            return Futures.successful(null);
        });
    }

    @Nonnull
    private static String getMapcodePrecision(@Nonnull final Mapcode mapcode, final int precision) {
        switch (precision) {
            case 1:
                return mapcode.getMapcodePrecision1();

            case 2:
                return mapcode.getMapcodePrecision2();

            default:
                return mapcode.getMapcodePrecision0();
        }
    }
}
