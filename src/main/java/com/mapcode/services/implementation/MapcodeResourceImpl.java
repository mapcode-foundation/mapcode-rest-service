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
 * This class implements the REST API that handles mapcode conversions.
 */
public class MapcodeResourceImpl implements MapcodeResource {
    private static final Logger LOG = LoggerFactory.getLogger(MapcodeResourceImpl.class);
    private static final Tracer TRACER = TracerFactory.getTracer(MapcodeResourceImpl.class, Tracer.class);

    private final ResourceProcessor processor;

    private final String listOfAllTerritoryCodes = Joiner.on('|').join(Arrays.asList(Territory.values()).stream().
            map(x -> x.toNameFormat(NameFormat.INTERNATIONAL)).collect(Collectors.toList()));
    private final String listOfAllTypes = Joiner.on('|').join(Arrays.asList(ParamType.values()).stream().
            map(x -> x).collect(Collectors.toList()));
    private final String listOfAllIncludes = Joiner.on('|').join(Arrays.asList(ParamInclude.values()).stream().
            map(x -> x).collect(Collectors.toList()));
    private final List<TerritoryDTO> listOfAllTerritories = Arrays.asList(Territory.values()).stream().
            map(x -> {
                final Territory parentTerritory = x.getParentTerritory();
                return new TerritoryDTO(
                        x.toNameFormat(NameFormat.INTERNATIONAL),
                        x.toNameFormat(NameFormat.MINIMAL_UNAMBIGUOUS),
                        x.toNameFormat(NameFormat.MINIMAL),
                        x.getTerritoryCode(),
                        x.getFullName(),
                        (parentTerritory == null) ? null : parentTerritory.toNameFormat(NameFormat.INTERNATIONAL),
                        x.getAliases(),
                        x.getFullNameAliases());
            }).
            collect(Collectors.toList());


    /**
     * The constructor is called by Google Guice at start-up time and gets a processor injected
     * to executed web requests on.
     *
     * @param processor Processor to process web requests on.
     */
    @Inject
    public MapcodeResourceImpl(@Nonnull final ResourceProcessor processor) {
        assert processor != null;
        this.processor = processor;
    }

    @Override
    public void convertLatLonToMapcode(@Nonnull final AsynchronousResponse response) throws ApiInvalidFormatException {

        // This method is forbidden. In REST terms, this should return ALL potential mapcodes - intractable.
        throw new ApiForbiddenException("Missing URL path parameters: /{lat,lon}/{" + listOfAllTypes.toLowerCase() + '}');
    }

    @Override
    public void convertLatLonToMapcode(
            final double paramLatDeg,
            final double paramLonDeg,
            final int paramPrecision,
            @Nullable final String paramTerritory,
            @Nonnull final String paramInclude,
            @Nonnull final AsynchronousResponse response) throws ApiInvalidFormatException {
        convertLatLonToMapcode(paramLatDeg, paramLonDeg, null, paramPrecision, paramTerritory, paramInclude, response);
    }

    @Override
    public void convertLatLonToMapcode(
            final double paramLatDeg,
            final double paramLonDeg,
            @Nullable final String paramType,
            final int paramPrecision,
            @Nullable final String paramTerritory,
            @Nonnull final String paramInclude,
            @Nonnull final AsynchronousResponse response) throws ApiInvalidFormatException {
        assert response != null;

        processor.process("convertLatLonToMapcode", LOG, response, () -> {
            LOG.debug("convertLatLonToMapcode: lat={}, lon={}, precision={}, type={}, context={}, include={}",
                    paramLatDeg, paramLonDeg, paramPrecision, paramType, paramTerritory, paramInclude);

            // Check lat range.
            final double latDeg = paramLatDeg;
            if (!MathUtils.isBetween(latDeg, ApiConstants.API_LAT_MIN, ApiConstants.API_LAT_MAX)) {
                throw new ApiInvalidFormatException(PARAM_LAT_DEG, String.valueOf(paramLatDeg),
                        "[" + ApiConstants.API_LAT_MIN + ", " + ApiConstants.API_LAT_MAX + ']');
            }

            // Check lon range.
            final double lonDeg = Geo.mapToLon(paramLonDeg);

            // Check precision.
            final int precision = paramPrecision;
            if (!MathUtils.isBetween(precision, 0, 2)) {
                throw new ApiInvalidFormatException(PARAM_PRECISION, String.valueOf(paramPrecision), "[0, 2]");
            }

            // Get the territory.
            final Territory territory = (paramTerritory != null) ? resolveTerritory(paramTerritory, null) : null;

            // Check type.
            ParamType type = null;
            if (paramType != null) {
                try {
                    type = ParamType.valueOf(paramType.toUpperCase());
                } catch (final IllegalArgumentException ignored) {
                    throw new ApiInvalidFormatException(PARAM_TYPE, paramType, listOfAllTypes.toLowerCase());
                }
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

            // Determine whether include=offset and territory=xxx were supplied as URL parameters.
            final boolean includeOffset = foundIncludeOffset;
            final boolean includeTerritory = foundIncludeTerritory;

            // Send a trace event with the lat/lon and other parameters.
            TRACER.eventLatLonToMapcode(latDeg, lonDeg, territory, precision, paramType, paramInclude);

            try {

                /**
                 * First get all mapcodes. This is not the most efficient implementation, as we encode the
                 * lat/lon 3 times, but unless there is a measured, significant performance issue in real life,
                 * this implementation is good enough for now (and pretty straightforward).
                 */

                // Get the shortest local mapcode.
                final Mapcode mapcodeLocal;
                if (territory == null) {
                    mapcodeLocal = MapcodeCodec.encodeToShortest(latDeg, lonDeg);
                } else {
                    mapcodeLocal = MapcodeCodec.encodeToShortest(latDeg, lonDeg, territory);
                }

                // Get the international mapcode.
                final Mapcode mapcodeInternational = MapcodeCodec.encodeToInternational(latDeg, lonDeg);

                // Get all mapcodes.
                final List<Mapcode> mapcodesAll;
                if (territory == null) {
                    mapcodesAll = MapcodeCodec.encode(latDeg, lonDeg);
                } else {
                    mapcodesAll = MapcodeCodec.encode(latDeg, lonDeg, territory);
                }

                // Create result body, which is an ApiDTO. The exact type of DTO is still to be deteremined below.
                final ApiDTO result;
                if (type == null) {

                    // No type was supplied, so we need to return the local, international and all mapcodes.
                    result = new MapcodesDTO(getMapcodeDTO(latDeg, lonDeg, precision, includeOffset, includeTerritory, mapcodeLocal),
                            getMapcodeDTO(latDeg, lonDeg, precision, includeOffset, includeTerritory, mapcodeInternational),
                            mapcodesAll.stream().
                                    map(mapcode -> getMapcodeDTO(latDeg, lonDeg, precision, includeOffset, includeTerritory, mapcode)).
                                    collect(Collectors.toList()));
                } else {

                    // Return only the local, international or all mapcodes.
                    switch (type) {
                        case LOCAL: {
                            result = getMapcodeDTO(latDeg, lonDeg, precision, includeOffset, includeTerritory, mapcodeLocal);
                            break;
                        }

                        case INTERNATIONAL: {
                            result = getMapcodeDTO(latDeg, lonDeg, precision, includeOffset, includeTerritory, mapcodeInternational);
                            break;
                        }

                        case MAPCODES: {
                            result = new MapcodeListDTO(mapcodesAll.stream().
                                    map(mapcode -> getMapcodeDTO(latDeg, lonDeg, precision, includeOffset, includeTerritory, mapcode)).
                                    collect(Collectors.toList()));
                            break;
                        }

                        default:
                            assert false;
                            result = null;
                    }
                }

                // Validate the DTO before returning it, to make sure it's valid (internal consistency check).
                result.validate();
                response.setResponse(Response.ok(result).build());
            } catch (final UnknownMapcodeException ignored) {

                // The mapcode conversion failed.
                throw new ApiNotFoundException("No mapcode found for lat=" + latDeg + ", lon=" + lonDeg + ", territory=" + territory);
            }

            // The response is already set within this method body.
            return Futures.successful(null);
        });
    }

    @Override
    public void convertMapcodeToLatLon(@Nonnull final AsynchronousResponse response) throws ApiNotFoundException, ApiInvalidFormatException {

        // This method is forbidden. In REST terms, this would return all world coordinates - intractable.
        throw new ApiForbiddenException("Missing URL path parameters: /{mapcode}");
    }

    @Override
    public void convertMapcodeToLatLon(
            @Nonnull final String paramCode,
            @Nullable final String paramTerritory,
            @Nonnull final AsynchronousResponse response) throws ApiNotFoundException, ApiInvalidFormatException {
        assert paramCode != null;
        assert response != null;

        processor.process("convertMapcodeToLatLon", LOG, response, () -> {
            LOG.debug("convertMapcodeToLatLon: code={}, territory={}", paramCode, paramTerritory);

            // Get the territory from the path (if specified).
            final Territory territory = (paramTerritory != null) ? resolveTerritory(paramTerritory, null) : null;

            // Check if the mapcode is correctly formatted.
            if (!Mapcode.isValidMapcodeFormat(paramCode)) {
                throw new ApiInvalidFormatException("mapcode", paramCode, Mapcode.REGEX_MAPCODE_FORMAT);
            }

            // Send a trace event with the mapcode and territory.
            TRACER.eventMapcodeToLatLon(paramCode, territory);

            // Create result body (always an ApiDTO).
            ApiDTO result;
            try {

                // Decode the actual mapcode.
                final Point point;
                if (territory == null) {
                    point = MapcodeCodec.decode(paramCode);
                } else {
                    point = MapcodeCodec.decode(paramCode, territory);
                }
                result = new PointDTO(point.getLatDeg(), point.getLonDeg());
            } catch (final UnknownMapcodeException ignored) {
                throw new ApiNotFoundException("No mapcode found for mapcode='" + paramCode + "', territory=" + territory);
            }

            // Validate the result (internal consistency check).
            result.validate();
            response.setResponse(Response.ok(result).build());

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

            // Check value of count.
            if (count < 0) {
                throw new ApiIntegerOutOfRangeException(PARAM_COUNT, count, 0, Integer.MAX_VALUE);
            }
            assert count >= 0;

            // Create the response and validate it.
            final int nrTerritories = listOfAllTerritories.size();
            final int fromIndex = (offset < 0) ? Math.max(0, nrTerritories + offset) : Math.min(nrTerritories, offset);
            final int toIndex = Math.min(nrTerritories, fromIndex + count);
            final TerritoriesDTO result = new TerritoriesDTO(listOfAllTerritories.subList(fromIndex, toIndex));

            // Validate the result (internal consistency check).
            result.validate();
            response.setResponse(Response.ok(result).build());

            // The response is already set within this method body.
            return Futures.successful(null);
        });
    }

    @Override
    public void getTerritory(
            @Nonnull final String paramTerritory,
            @Nullable final String paramContext,
            @Nonnull final AsynchronousResponse response) throws ApiInvalidFormatException {
        assert paramTerritory != null;
        assert response != null;

        processor.process("getTerritory", LOG, response, () -> {
            LOG.debug("getTerritory: territory={}, context={}", paramTerritory, paramContext);

            // Get the territory from the URL.
            final Territory territory = resolveTerritory(paramTerritory, paramContext);

            // Return the right territory information.
            final Territory parentTerritory = territory.getParentTerritory();
            final TerritoryDTO result = new TerritoryDTO(
                    territory.toNameFormat(NameFormat.INTERNATIONAL),
                    territory.toNameFormat(NameFormat.MINIMAL_UNAMBIGUOUS),
                    territory.toNameFormat(NameFormat.MINIMAL),
                    territory.getTerritoryCode(),
                    territory.getFullName(),
                    (parentTerritory == null) ? null : parentTerritory.toNameFormat(NameFormat.INTERNATIONAL),
                    territory.getAliases(),
                    territory.getFullNameAliases()
            );

            // Validate the result (internal consistency check).
            result.validate();
            response.setResponse(Response.ok(result).build());

            // The response is already set within this method body.
            return Futures.successful(null);
        });
    }

    @Nonnull
    private Territory resolveTerritory(@Nonnull final String paramTerritory, @Nullable final String paramParent) {
        ParentTerritory parentTerritory;
        if (paramParent != null) {

            // Try to use the parent territory, if available.
            Territory context = null;
            try {

                context = Territory.valueOf(paramParent.replace('-', '_').toUpperCase());
                if (context.getParentTerritory() != null) {
                    context = context.getParentTerritory();
                }
            } catch (final IllegalArgumentException ignored) {

                // Check if it was an alias.
                context = getTerritoryAlias(paramParent);
                if (context == null) {
                    throw new ApiInvalidFormatException("parent", paramParent, listOfAllTerritoryCodes);
                }
            }

            // Use the resolved context.
            try {
                parentTerritory = ParentTerritory.valueOf(context.toString());
            } catch (final IllegalArgumentException ignored) {

                // Explicitly remove parent context.
                parentTerritory = null;
            }
        } else {
            parentTerritory = null;
        }

        // First try if its a numeric territory code.
        try {
            final int territoryCode = Integer.valueOf(paramTerritory);

            // Try and convert it as an integer - this throws an exception for non-int codes.
            return Territory.fromTerritoryCode(territoryCode);
        } catch (final IllegalArgumentException ignore) {

            // Territory code failed. Now, try to convert it as an ISO-code.
            try {
                if (parentTerritory != null) {

                    // Parent was specified.
                    return Territory.fromString(paramTerritory.toUpperCase(), parentTerritory);
                } else {

                    // No parent available.
                    return Territory.fromString(paramTerritory.toUpperCase());
                }
            } catch (final UnknownTerritoryException ignored) {
                throw new ApiInvalidFormatException("territory", paramTerritory, listOfAllTerritoryCodes);
            }
        }
    }

    @Nullable
    private static Territory getTerritoryAlias(@Nonnull final String paramAlias) {
        Territory context = null;
        final String aliasToLookFor = paramAlias.replace('_', '-').toUpperCase();
        for (final Territory aliasTerritory : Territory.values()) {
            if (Arrays.asList(aliasTerritory.getAliases()).contains(aliasToLookFor)) {
                context = aliasTerritory;
                break;
            }
        }
        return context;
    }

    @Nonnull
    private static MapcodeDTO getMapcodeDTO(final double latDeg, final double lonDeg, final int precision,
                                            final boolean includeOffset, final boolean includeTerritory,
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
                return mapcode.getMapcodePrecision(1);

            case 2:
                return mapcode.getMapcodePrecision(2);

            default:
                return mapcode.getMapcodePrecision(0);
        }
    }

    /**
     * This interface defines a Tracer interface for mapcode service events.
     */
    public interface Tracer extends Traceable {

        // A request to translate a lat/lon to a mapcode is made.
        void eventLatLonToMapcode(double latDeg, double lonDeg, @Nullable Territory territory,
                                  int precision, @Nullable String type, @Nonnull String include);

        // A request to translate a mapcode to a lat/lon is made.
        void eventMapcodeToLatLon(@Nonnull String code, @Nullable Territory territory);
    }
}

