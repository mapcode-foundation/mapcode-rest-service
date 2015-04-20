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
import com.tomtom.speedtools.apivalidation.exceptions.ApiIntegerOutOfRangeException;
import com.tomtom.speedtools.apivalidation.exceptions.ApiInvalidFormatException;
import com.tomtom.speedtools.apivalidation.exceptions.ApiNotFoundException;
import com.tomtom.speedtools.geometry.Geo;
import com.tomtom.speedtools.geometry.GeoPoint;
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

    private final ResourceProcessor processor;
    private final String listOfAllTerritories;
    private final String listOfAllTypes;
    private final String listOfAllIncludes;

    @Inject
    public MapcodeResourceImpl(@Nonnull final ResourceProcessor processor) {
        assert processor != null;
        this.processor = processor;

        boolean first = true;
        StringBuilder sb = new StringBuilder();
        for (final Territory territory : Territory.values()) {
            if (first) {
                first = false;
            } else {
                sb.append('|');
            }
            sb.append(territory.toNameFormat(NameFormat.MINIMAL_UNAMBIGUOUS));
            sb.append('=');
            sb.append(territory.getTerritoryCode());
        }
        listOfAllTerritories = sb.toString();

        first = true;
        sb = new StringBuilder();
        for (final ParamType paramType : ParamType.values()) {
            if (first) {
                first = false;
            } else {
                sb.append('|');
            }
            sb.append(paramType.toString().toLowerCase());
        }
        listOfAllTypes = sb.toString();

        first = true;
        sb = new StringBuilder();
        for (final ParamInclude paramInclude : ParamInclude.values()) {
            if (first) {
                first = false;
            } else {
                sb.append('|');
            }
            sb.append(paramInclude.toString().toLowerCase());
        }
        listOfAllIncludes = sb.toString();
    }

    @Override
    public void convertLatLonToMapcodeAll(
            final double paramLatDeg,
            final double paramLonDeg,
            final int paramPrecision,
            @Nullable final String paramTerritory,
            @Nonnull final AsynchronousResponse response) throws ApiNotFoundException {
        throw new ApiNotFoundException("Specify: " + listOfAllTypes);
    }

    @Override
    public void convertLatLonToMapcode(
            final double paramLatDeg,
            final double paramLonDeg,
            final int paramPrecision,
            @Nonnull final String paramType,
            @Nullable final String paramTerritory,
            @Nonnull final String paramInclude,
            @Nonnull final AsynchronousResponse response) throws ApiInvalidFormatException {
        assert paramType != null;
        assert response != null;

        processor.process("convertLatLonToMapcode", LOG, response, () -> {
            LOG.debug("convertLatLonToMapcode: lat={}, lon={}, precision={}, type={}, territory={}, include={}",
                    paramLatDeg, paramLonDeg, paramPrecision, paramType, paramTerritory, paramInclude);

            // Check lat.
            final double latDeg = paramLatDeg;
            if (!MathUtils.isBetween(latDeg, ApiConstants.API_LAT_MIN, ApiConstants.API_LAT_MAX)) {
                throw new ApiInvalidFormatException(PARAM_LAT_DEG, String.valueOf(paramLatDeg),
                        "[" + ApiConstants.API_LAT_MIN + ", " + ApiConstants.API_LAT_MAX + ']');
            }

            // Check lon.
            final double lonDeg = Geo.mapToLon(paramLonDeg);

            // Check precision.
            final int precision = paramPrecision;
            if (!MathUtils.isBetween(precision, 0, 2)) {
                throw new ApiInvalidFormatException(PARAM_PRECISION, String.valueOf(paramPrecision), "[0, 2]");
            }

            // Check territory.
            Territory territory = null;
            if (paramTerritory != null) {
                try {
                    final int territoryCode = Integer.valueOf(paramTerritory);
                    territory = Territory.fromTerritoryCode(territoryCode);
                } catch (final IllegalArgumentException ignored) {
                    try {
                        territory = Territory.fromString(paramTerritory);
                    } catch (final UnknownTerritoryException ignored2) {
                        throw new ApiInvalidFormatException(PARAM_TERRITORY, paramTerritory, listOfAllTerritories);
                    }
                }
            }

            // Check type.
            ParamType type;
            try {
                type = ParamType.valueOf(paramType.toUpperCase());
            } catch (final IllegalArgumentException ignored) {
                throw new ApiInvalidFormatException(PARAM_TYPE, paramType, listOfAllTypes);
            }

            // Check include.
            ParamInclude include;
            try {
                include = ParamInclude.valueOf(paramInclude.toUpperCase());
            } catch (final IllegalArgumentException ignored) {
                throw new ApiInvalidFormatException(PARAM_INCLUDE, paramInclude, listOfAllIncludes);
            }

            // Create result body.
            ApiDataBinder binder;
            switch (type) {
                case ALL: {
                    final List<MapcodeBinder> list = new ArrayList<>();
                    final List<Mapcode> mapcodes;
                    if (territory == null) {
                        mapcodes = MapcodeCodec.encode(latDeg, lonDeg);
                    } else {
                        mapcodes = MapcodeCodec.encode(latDeg, lonDeg, territory);
                    }
                    for (final Mapcode mapcode : mapcodes) {
                        final MapcodeBinder item = new MapcodeBinder(
                                getMapcodePrecision(mapcode, precision),
                                mapcode.getTerritory(),
                                (include == ParamInclude.OFFSET) ? offsetFromLatLonInMeters(latDeg, lonDeg, mapcode) : null);
                        list.add(item);
                    }
                    final MapcodesBinder mapcodesBinder = new MapcodesBinder(list);
                    mapcodesBinder.validate();
                    binder = mapcodesBinder;
                    break;
                }

                case LOCAL: {
                    final Mapcode mapcode;
                    if (territory == null) {
                        mapcode = MapcodeCodec.encodeToShortest(latDeg, lonDeg);
                    } else {
                        mapcode = MapcodeCodec.encodeToShortest(latDeg, lonDeg, territory);
                    }
                    final MapcodeBinder mapcodeBinder = new MapcodeBinder(
                            getMapcodePrecision(mapcode, precision),
                            mapcode.getTerritory(),
                            (include == ParamInclude.OFFSET) ? offsetFromLatLonInMeters(latDeg, lonDeg, mapcode) : null);
                    mapcodeBinder.validate();
                    binder = mapcodeBinder;
                    break;
                }

                case INTERNATIONAL: {
                    final List<Mapcode> mapcodes = MapcodeCodec.encode(latDeg, lonDeg);
                    final Mapcode mapcode = mapcodes.get(mapcodes.size() - 1);
                    final MapcodeBinder mapcodeBinder = new MapcodeBinder(
                            getMapcodePrecision(mapcode, precision),
                            mapcode.getTerritory(),
                            (include == ParamInclude.OFFSET) ? offsetFromLatLonInMeters(latDeg, lonDeg, mapcode) : null);
                    mapcodeBinder.validate();
                    binder = mapcodeBinder;
                    break;
                }

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
            @Nonnull final String paramMapcode,
            @Nullable final String paramTerritory,
            @Nonnull final AsynchronousResponse response) throws ApiNotFoundException, ApiInvalidFormatException {
        assert paramMapcode != null;
        assert response != null;

        processor.process("convertMapcodeToLatLon", LOG, response, () -> {
            LOG.debug("convertMapcodeToLatLon: mapcode={}, territory={}", paramMapcode, paramTerritory);

            Territory territory = null;
            if (paramTerritory != null) {
                try {
                    final int territoryCode = Integer.valueOf(paramTerritory);
                    territory = Territory.fromTerritoryCode(territoryCode);
                } catch (final IllegalArgumentException ignored) {
                    try {
                        territory = Territory.fromString(paramTerritory);
                    } catch (final UnknownTerritoryException ignored2) {
                        throw new ApiInvalidFormatException("territory", paramTerritory, listOfAllTerritories);
                    }
                }
            }

            // Create result body.
            try {
                final Point point;
                if (territory == null) {
                    point = MapcodeCodec.decode(paramMapcode);
                } else {
                    point = MapcodeCodec.decode(paramMapcode, territory);
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
    public void getTerritories(
            final int offset,
            final int count,
            @Nonnull final AsynchronousResponse response) throws ApiIntegerOutOfRangeException {
        assert response != null;

        processor.process("getTerritories", LOG, response, () -> {
            LOG.debug("getTerritories");

            if (count < 0) {
                throw new ApiIntegerOutOfRangeException(PARAM_COUNT, count, 0, Integer.MAX_VALUE);
            }
            assert count >= 0;

            final List<TerritoryBinder> allTerritories = new ArrayList<>();
            for (final Territory territory : Territory.values()) {
                final Territory parentTerritory = territory.getParentTerritory();
                final TerritoryBinder binder = new TerritoryBinder(
                        territory.getTerritoryCode(),
                        territory.getFullName(),
                        (parentTerritory == null) ? null : parentTerritory.toNameFormat(NameFormat.MINIMAL_UNAMBIGUOUS),
                        territory.getAliases(),
                        territory.getFullNameAliases()
                );
                allTerritories.add(binder);
            }

            // Create the response and validate it.
            final int nrTerritories = allTerritories.size();
            final int fromIndex = (offset < 0) ? Math.max(0, nrTerritories + offset) : Math.min(nrTerritories, offset);
            final int toIndex = Math.min(nrTerritories, fromIndex + count);
            final TerritoriesBinder binder = new TerritoriesBinder(allTerritories.subList(fromIndex, toIndex));
            binder.validate();
            response.setResponse(Response.ok(binder).build());

            // The response is already set within this method body.
            return Futures.successful(null);
        });
    }

    @Override
    public void getTerritory(
            @Nullable final String paramTerritory,
            @Nonnull final AsynchronousResponse response) throws ApiInvalidFormatException {
        assert paramTerritory != null;
        assert response != null;

        processor.process("getTerritory", LOG, response, () -> {
            LOG.debug("getTerritory: territory={}", paramTerritory);

            Territory territory = null;
            try {
                final int territoryCode = Integer.valueOf(paramTerritory);
                territory = Territory.fromTerritoryCode(territoryCode);
            } catch (final IllegalArgumentException ignored) {
                try {
                    territory = Territory.fromString(paramTerritory);
                } catch (final UnknownTerritoryException ignored2) {
                    throw new ApiInvalidFormatException("territory", paramTerritory, listOfAllTerritories);
                }
            }

            final Territory parentTerritory = territory.getParentTerritory();
            final TerritoryBinder binder = new TerritoryBinder(
                    territory.getTerritoryCode(),
                    territory.getFullName(),
                    (parentTerritory == null) ? null : parentTerritory.toNameFormat(NameFormat.MINIMAL_UNAMBIGUOUS),
                    territory.getAliases(),
                    territory.getFullNameAliases()
            );
            binder.validate();
            response.setResponse(Response.ok(binder).build());

            // The response is already set within this method body.
            return Futures.successful(null);
        });
    }

    private static double offsetFromLatLonInMeters(
            final double latDeg,
            final double lonDeg,
            @Nonnull final Mapcode mapcode) throws UnknownMapcodeException {
        assert mapcode != null;
        final GeoPoint position = new GeoPoint(latDeg, lonDeg);
        final Point point = MapcodeCodec.decode(mapcode.getMapcode(), mapcode.getTerritory());
        final GeoPoint center = new GeoPoint(point.getLatDeg(), point.getLonDeg());
        final double distanceMeters = Geo.distanceInMeters(position, center);
        return distanceMeters;
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
