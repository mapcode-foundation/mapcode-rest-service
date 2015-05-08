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
import com.google.common.base.Joiner;
import com.mapcode.*;
import com.mapcode.Territory.NameFormat;
import com.mapcode.services.ApiConstants;
import com.mapcode.services.MapcodeResource;
import com.mapcode.services.dto.*;
import com.tomtom.speedtools.apivalidation.ApiDTO;
import com.tomtom.speedtools.apivalidation.exceptions.ApiForbiddenException;
import com.tomtom.speedtools.apivalidation.exceptions.ApiIntegerOutOfRangeException;
import com.tomtom.speedtools.apivalidation.exceptions.ApiInvalidFormatException;
import com.tomtom.speedtools.apivalidation.exceptions.ApiNotFoundException;
import com.tomtom.speedtools.geometry.Geo;
import com.tomtom.speedtools.geometry.GeoPoint;
import com.tomtom.speedtools.rest.ResourceProcessor;
import com.tomtom.speedtools.tracer.Traceable;
import com.tomtom.speedtools.tracer.TracerFactory;
import com.tomtom.speedtools.utils.MathUtils;
import org.jboss.resteasy.spi.AsynchronousResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class implements the REST API that deals with TTBin files.
 */
public class MapcodeResourceImpl implements MapcodeResource {
    private static final Logger LOG = LoggerFactory.getLogger(MapcodeResourceImpl.class);
    private static final Tracer TRACER = TracerFactory.getTracer(MapcodeResourceImpl.class, Tracer.class);

    private final ResourceProcessor processor;
    private final String listOfAllTerritories = Joiner.on('|').join(Arrays.asList(Territory.values()).stream().
            map(x -> x.toNameFormat(NameFormat.MINIMAL_UNAMBIGUOUS)).collect(Collectors.toList()));
    private final String listOfAllTypes = Joiner.on('|').join(Arrays.asList(ParamType.values()).stream().
            map(x -> x).collect(Collectors.toList()));
    private final String listOfAllIncludes = Joiner.on('|').join(Arrays.asList(ParamInclude.values()).stream().
            map(x -> x).collect(Collectors.toList()));

    @Inject
    public MapcodeResourceImpl(@Nonnull final ResourceProcessor processor) {
        assert processor != null;
        this.processor = processor;
    }

    @Override
    public void convertLatLonToMapcode(
            @Nonnull final AsynchronousResponse response) throws ApiInvalidFormatException {
        throw new ApiForbiddenException("Missing URL path parameters: /{lat,lon}/{" + listOfAllTypes.toLowerCase() + '}');
    }

    @Override
    public void convertLatLonToMapcode(
            final double paramLatDeg,
            final double paramLonDeg,
            @Nonnull final AsynchronousResponse response) throws ApiInvalidFormatException {
        throw new ApiForbiddenException("Missing URL path parameter: /{" + listOfAllTypes.toLowerCase() + '}');
    }

    @Override
    public void convertLatLonToMapcode(
            final double paramLatDeg,
            final double paramLonDeg,
            @Nonnull final String paramType,
            final int paramPrecision,
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
                throw new ApiInvalidFormatException(PARAM_TYPE, paramType, listOfAllTypes.toLowerCase());
            }

            // Check include.
            boolean foundIncludeOffset = false;
            boolean foundIncludeTerritory = false;
            for (final String arg : paramInclude.toUpperCase().split(",")) {
                if (!arg.isEmpty()) {
                    try {
                        ParamInclude include = ParamInclude.valueOf(arg);
                        foundIncludeOffset = foundIncludeOffset || (include == ParamInclude.OFFSET);
                        foundIncludeTerritory = foundIncludeTerritory || (include == ParamInclude.TERRITORY);
                    } catch (final IllegalArgumentException ignored) {
                        throw new ApiInvalidFormatException(PARAM_INCLUDE, paramInclude, listOfAllIncludes.toLowerCase());
                    }
                }
            }
            final boolean includeOffset = foundIncludeOffset;
            final boolean includeTerritory = foundIncludeTerritory;
            TRACER.eventLatLonToMapcode(latDeg, lonDeg, territory, precision, paramType, paramInclude);

            try {
                // Create result body.
                ApiDTO result;
                switch (type) {
                    case ALL: {
                        final List<Mapcode> mapcodes;
                        if (territory == null) {
                            mapcodes = MapcodeCodec.encode(latDeg, lonDeg);
                        } else {
                            mapcodes = MapcodeCodec.encode(latDeg, lonDeg, territory);
                        }
                        result = new MapcodesDTO(mapcodes.stream().
                                map(mapcode -> getMapcodeDTO(latDeg, lonDeg, precision, includeOffset, includeTerritory, mapcode)).
                                collect(Collectors.toList()));
                        break;
                    }

                    case LOCAL: {
                        final Mapcode mapcode;
                        if (territory == null) {
                            mapcode = MapcodeCodec.encodeToShortest(latDeg, lonDeg);
                        } else {
                            mapcode = MapcodeCodec.encodeToShortest(latDeg, lonDeg, territory);
                        }
                        result = getMapcodeDTO(latDeg, lonDeg, precision, includeOffset, includeTerritory, mapcode);
                        break;
                    }

                    case INTERNATIONAL: {
                        final List<Mapcode> mapcodes = MapcodeCodec.encode(latDeg, lonDeg);
                        final Mapcode mapcode = mapcodes.get(mapcodes.size() - 1);
                        result = getMapcodeDTO(latDeg, lonDeg, precision, includeOffset, includeTerritory, mapcode);
                        break;
                    }

                    default:
                        assert false;
                        result = null;
                }
                result.validate();
                response.setResponse(Response.ok(result).build());
            } catch (final UnknownMapcodeException ignored) {
                throw new ApiNotFoundException("No mapcode found for lat=" + latDeg + ", lon=" + lonDeg + ", territory=" + territory);
            }

            // The response is already set within this method body.
            return Futures.successful(null);
        });
    }

    @Override
    public void convertMapcodeToLatLon(
            @Nonnull final AsynchronousResponse response) throws ApiNotFoundException, ApiInvalidFormatException {
        throw new ApiForbiddenException("Missing URL path parameters: /{mapcode}");
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
            TRACER.eventMapcodeToLatLon(paramMapcode, territory);

            // Create result body.
            try {
                final Point point;
                if (territory == null) {
                    point = MapcodeCodec.decode(paramMapcode);
                } else {
                    point = MapcodeCodec.decode(paramMapcode, territory);
                }
                final PointDTO result = new PointDTO(point.getLatDeg(), point.getLonDeg());
                result.validate();
                response.setResponse(Response.ok(result).build());
            } catch (final UnknownMapcodeException e) {
                throw new ApiNotFoundException("No mapcode found for mapcode='" + paramMapcode + "', territory=" + territory);
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

            Joiner.on('|').join(Arrays.asList(Territory.values()).stream().
                    map(x -> x.toNameFormat(NameFormat.MINIMAL_UNAMBIGUOUS)).collect(Collectors.toList()));

            final List<TerritoryDTO> allTerritories = Arrays.asList(Territory.values()).stream().
                    map(x -> {
                        final Territory parentTerritory = x.getParentTerritory();
                        return new TerritoryDTO(
                                x.getTerritoryCode(),
                                x.getFullName(),
                                (parentTerritory == null) ? null : parentTerritory.toNameFormat(NameFormat.MINIMAL_UNAMBIGUOUS),
                                x.getAliases(),
                                x.getFullNameAliases());
                    }).
                    collect(Collectors.toList());

            // Create the response and validate it.
            final int nrTerritories = allTerritories.size();
            final int fromIndex = (offset < 0) ? Math.max(0, nrTerritories + offset) : Math.min(nrTerritories, offset);
            final int toIndex = Math.min(nrTerritories, fromIndex + count);
            final TerritoriesDTO result = new TerritoriesDTO(allTerritories.subList(fromIndex, toIndex));
            result.validate();
            response.setResponse(Response.ok(result).build());

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
            final TerritoryDTO result = new TerritoryDTO(
                    territory.getTerritoryCode(),
                    territory.getFullName(),
                    (parentTerritory == null) ? null : parentTerritory.toNameFormat(NameFormat.MINIMAL_UNAMBIGUOUS),
                    territory.getAliases(),
                    territory.getFullNameAliases()
            );
            result.validate();
            response.setResponse(Response.ok(result).build());

            // The response is already set within this method body.
            return Futures.successful(null);
        });
    }

    @Nonnull
    private static MapcodeDTO getMapcodeDTO(double latDeg, double lonDeg, int precision,
                                            boolean includeOffset, boolean includeTerritory,
                                            @Nonnull final Mapcode mapcode) {
        return new MapcodeDTO(
                getMapcodePrecision(mapcode, precision),
                (includeTerritory || (mapcode.getTerritory() != Territory.AAA)) ?
                        mapcode.getTerritory().toNameFormat(NameFormat.INTERNATIONAL) : null,
                includeOffset ? offsetFromLatLonInMeters(latDeg, lonDeg, mapcode, precision) : null);
    }

    private static double offsetFromLatLonInMeters(
            final double latDeg,
            final double lonDeg,
            @Nonnull final Mapcode mapcode,
            final int precision) {
        assert mapcode != null;
        final GeoPoint position = new GeoPoint(latDeg, lonDeg);
        try {
            final Point point = MapcodeCodec.decode(getMapcodePrecision(mapcode, precision), mapcode.getTerritory());
            final GeoPoint center = new GeoPoint(point.getLatDeg(), point.getLonDeg());
            final double distanceMeters = Geo.distanceInMeters(position, center);
            return Math.round(distanceMeters * 100.0) / 100.0;
        } catch (final UnknownMapcodeException ignore) {
            // Simply ignore.
            return 0.0;
        }
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

    /**
     * This interface defines a Tracer interface for mapcode service events.
     */
    public interface Tracer extends Traceable {

        // A request to translate a lat/lon to a mapcode is made.
        void eventLatLonToMapcode(double latDeg, double lonDeg, @Nullable Territory territory,
                                  int precision, @Nonnull String type, @Nonnull String include);

        // A request to translate a mapcode to a lat/lon is made.
        void eventMapcodeToLatLon(@Nonnull String mapcode, @Nullable Territory territory);
    }
}

