/*
 * Copyright (C) 2016-2017, Stichting Mapcode Foundation (http://www.mapcode.com)
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

import akka.dispatch.Futures;
import com.google.common.base.Joiner;
import com.mapcode.*;
import com.mapcode.Territory.AlphaCodeFormat;
import com.mapcode.services.ApiConstants;
import com.mapcode.services.MapcodeResource;
import com.mapcode.services.dto.*;
import com.mapcode.services.metrics.SystemMetricsCollector;
import com.tomtom.speedtools.apivalidation.ApiDTO;
import com.tomtom.speedtools.apivalidation.exceptions.ApiForbiddenException;
import com.tomtom.speedtools.apivalidation.exceptions.ApiIntegerOutOfRangeException;
import com.tomtom.speedtools.apivalidation.exceptions.ApiInvalidFormatException;
import com.tomtom.speedtools.apivalidation.exceptions.ApiNotFoundException;
import com.tomtom.speedtools.geometry.Geo;
import com.tomtom.speedtools.geometry.GeoPoint;
import com.tomtom.speedtools.objects.Tuple;
import com.tomtom.speedtools.rest.ResourceProcessor;
import com.tomtom.speedtools.time.UTCTime;
import com.tomtom.speedtools.tracer.Traceable;
import com.tomtom.speedtools.tracer.TracerFactory;
import com.tomtom.speedtools.utils.MathUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
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
    private final SystemMetricsCollector metricsCollector;

    private static final String API_ERROR_VALID_TERRITORY_CODES = Joiner.on('|').join(Arrays.asList(Territory.values()).stream().
            map(x -> (x.toString())).collect(Collectors.toList()));

    private static final String API_ERROR_VALID_ALPHABET_CODES = Joiner.on('|').join(Arrays.asList(Alphabet.values()).stream().
            map(x -> (x.toString())).collect(Collectors.toList()));

    private static final String API_ERROR_VALID_TYPES = Joiner.on('|').join(Arrays.asList(ParamType.values()).stream().
            map(x -> x).collect(Collectors.toList()));

    private static final String API_ERROR_VALID_INCLUDES = Joiner.on('|').join(Arrays.asList(ParamInclude.values()).stream().
            map(x -> x).collect(Collectors.toList()));

    private static final TerritoryListDTO ALL_TERRITORY_DTO = new TerritoryListDTO(Territory.values());
    private static final AlphabetListDTO ALL_ALPHABET_DTO = new AlphabetListDTO(Alphabet.values());

    /**
     * The constructor is called by Google Guice at start-up time and gets a processor injected
     * to executed web requests on.
     *
     * @param processor        Processor to process web requests on.
     * @param metricsCollector Metric collector.
     */
    @Inject
    public MapcodeResourceImpl(
            @Nonnull final ResourceProcessor processor,
            @Nonnull final SystemMetricsCollector metricsCollector) {
        assert processor != null;
        assert metricsCollector != null;
        this.processor = processor;
        this.metricsCollector = metricsCollector;
    }

    @Override
    public void convertLatLonToMapcode(
            @Nonnull final AsyncResponse response) throws ApiInvalidFormatException {

        // This method is forbidden. In REST terms, this should return ALL potential mapcodes - intractable.
        processor.process("convertLatLonToMapcode", LOG, response, () -> {
            throw new ApiForbiddenException("Missing URL path parameters: /{lat,lon}/{" + API_ERROR_VALID_TYPES.toLowerCase() + '}');
        });
    }

    @Override
    public void convertLatLonToMapcodeXml(
            @Suspended @Nonnull final AsyncResponse response) throws ApiInvalidFormatException {
        convertLatLonToMapcode(response);
    }

    @Override
    public void convertLatLonToMapcodeJson(
            @Suspended @Nonnull final AsyncResponse response) throws ApiInvalidFormatException {
        convertLatLonToMapcode(response);
    }

    @Override
    public void convertLatLonToMapcode(
            final double paramLatDeg,
            final double paramLonDeg,
            final int paramPrecision,
            @Nullable final String paramTerritory,
            @Nullable final String paramContextMustBeNull,
            @Nullable final String paramAlphabet,
            @Nonnull final String paramInclude,
            @Nonnull final String paramClient,
            @Nonnull final String paramAllowLog,
            @Nonnull final AsyncResponse response) throws ApiInvalidFormatException {
        convertLatLonToMapcode(paramLatDeg, paramLonDeg, null, paramPrecision, paramTerritory, paramContextMustBeNull,
                paramAlphabet, paramInclude, paramClient, paramAllowLog, response);
    }

    @Override
    public void convertLatLonToMapcodeXml(
            final double paramLatDeg,
            final double paramLonDeg,
            final int paramPrecision,
            @Nullable final String paramTerritory,
            @Nullable final String paramContextMustBeNull,
            @Nullable final String paramAlphabet,
            @Nonnull final String paramInclude,
            @Nonnull final String paramClient,
            @Nonnull final String paramAllowLog,
            @Suspended @Nonnull final AsyncResponse response)
            throws ApiInvalidFormatException {
        convertLatLonToMapcode(paramLatDeg, paramLonDeg, paramPrecision, paramTerritory, paramContextMustBeNull,
                paramAlphabet, paramInclude, paramClient, paramAllowLog, response);
    }

    @Override
    public void convertLatLonToMapcodeJson(
            final double paramLatDeg,
            final double paramLonDeg,
            final int paramPrecision,
            @Nullable final String paramTerritory,
            @Nullable final String paramContextMustBeNull,
            @Nullable final String paramAlphabet,
            @Nonnull final String paramInclude,
            @Nonnull final String paramClient,
            @Nonnull final String paramAllowLog,
            @Suspended @Nonnull final AsyncResponse response)
            throws ApiInvalidFormatException {
        convertLatLonToMapcode(paramLatDeg, paramLonDeg, paramPrecision, paramTerritory, paramContextMustBeNull,
                paramAlphabet, paramInclude, paramClient, paramAllowLog, response);
    }

    @Override
    public void convertLatLonToMapcode(
            final double paramLatDeg,
            final double paramLonDeg,
            @Nullable final String paramType,
            final int paramPrecision,
            @Nullable final String paramTerritory,
            @Nullable final String paramContextMustBeNull,
            @Nullable final String paramAlphabet,
            @Nonnull final String paramInclude,
            @Nonnull final String paramClient,
            @Nonnull final String paramAllowLog,
            @Nonnull final AsyncResponse response) throws ApiInvalidFormatException {
        assert response != null;

        processor.process("convertLatLonToMapcode", LOG, response, () -> {
            // Get debug mode.
            final boolean allowLog = "true".equalsIgnoreCase(paramAllowLog);

            LOG.info("convertLatLonToMapcode: lat={}, lon={}, precision={}, type={}, context={}, alphabet={}, include={}, client={}, allowLog={}",
                    paramLatDeg, paramLonDeg, paramPrecision, paramType, paramTerritory, paramAlphabet, paramInclude, paramClient, paramAllowLog);
            metricsCollector.addOneLatLonToMapcodeRequest(paramClient);

            // Prevent 'context' from inadvertently being specified.
            if (paramContextMustBeNull != null) {
                throw new ApiInvalidFormatException(PARAM_CONTEXT, paramContextMustBeNull, "null");
            }

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
            if (!MathUtils.isBetween(precision, ApiConstants.API_PRECISION_MIN, ApiConstants.API_PRECISION_MAX)) {
                throw new ApiInvalidFormatException(PARAM_PRECISION, String.valueOf(paramPrecision), "[" + ApiConstants.API_PRECISION_MIN +
                        ", " + ApiConstants.API_PRECISION_MAX + ']');
            }

            // Get the territory.
            @Nullable final Territory territory;
            try {
                territory = (paramTerritory != null) ?
                        resolveTerritory(StringEscapeUtils.unescapeHtml4(paramTerritory), null) : null;
            } catch (final IllegalArgumentException ignored) {
                throw new ApiInvalidFormatException(PARAM_TERRITORY, paramTerritory, API_ERROR_VALID_TERRITORY_CODES);
            }

            // Get the alphabet.
            final Alphabet alphabet;
            try {
                alphabet = (paramAlphabet != null) ? Alphabet.fromString(paramAlphabet) : null;
            } catch (final IllegalArgumentException ignored) {
                throw new ApiInvalidFormatException(PARAM_ALPHABET, paramAlphabet, API_ERROR_VALID_ALPHABET_CODES);
            }

            // Check type.
            ParamType type = null;
            if (paramType != null) {
                try {
                    type = ParamType.valueOf(paramType.toUpperCase());
                } catch (final IllegalArgumentException ignored) {
                    throw new ApiInvalidFormatException(PARAM_TYPE, paramType, API_ERROR_VALID_TYPES.toLowerCase());
                }
            }

            // Need to store in finals, for lambda functions.
            final boolean includeOffset;
            final boolean includeTerritory;
            final boolean includeAlphabet;
            final boolean includeRectangle;
            {
                // Determine whether include=offset and territory=xxx were supplied as URL parameters.
                boolean foundIncludeOffset = false;
                boolean foundIncludeTerritory = false;
                boolean foundIncludeAlphabet = false;
                boolean foundIncludeRectangle = false;
                for (final String arg : paramInclude.toUpperCase().split(",")) {
                    if (!arg.isEmpty()) {
                        try {
                            final ParamInclude include = ParamInclude.valueOf(arg);
                            foundIncludeOffset = foundIncludeOffset || (include == ParamInclude.OFFSET);
                            foundIncludeTerritory = foundIncludeTerritory || (include == ParamInclude.TERRITORY);
                            foundIncludeAlphabet = foundIncludeAlphabet || (include == ParamInclude.ALPHABET);
                            foundIncludeRectangle = foundIncludeRectangle || (include == ParamInclude.RECTANGLE);
                        } catch (final IllegalArgumentException ignored) {
                            throw new ApiInvalidFormatException(PARAM_INCLUDE, paramInclude, API_ERROR_VALID_INCLUDES.toLowerCase());
                        }
                    }
                }

                // Need to store in finals, for lambda functions.
                includeOffset = foundIncludeOffset;
                includeTerritory = foundIncludeTerritory;
                includeAlphabet = foundIncludeAlphabet;
                includeRectangle = foundIncludeRectangle;
            }

            // Send a trace event with the lat/lon and other parameters.
            if (allowLog) {
                TRACER.eventLatLonToMapcode(latDeg, lonDeg, territory, precision, paramType,
                        paramAlphabet, paramInclude, UTCTime.now(), paramClient);
            }

            final List<Tuple<Mapcode, Rectangle>> mapcodesAndRectangles = new ArrayList<>();
            final Tuple<Mapcode, Rectangle> mapcodeInternationalAndRectangle;
            final Tuple<Mapcode, Rectangle> mapcodeLocalAndRectangle;
            try {

                /**
                 * First get all mapcodes. This is not the most efficient implementation, as we encode the
                 * lat/lon 3 times, but unless there is a measured, significant performance issue in real life,
                 * this implementation is good enough for now (and pretty straightforward).
                 */

                // Get all mapcodes.
                final List<Mapcode> mapcodes;
                mapcodes = MapcodeCodec.encode(latDeg, lonDeg, territory);
                mapcodes.stream().forEach(mapcode -> {
                    try {
                        final Rectangle rectangle = MapcodeCodec.decodeToRectangle(mapcode.getCode(), mapcode.getTerritory());
                        mapcodesAndRectangles.add(Tuple.create(mapcode, rectangle));
                    } catch (final UnknownMapcodeException e) {
                        LOG.warn("convertLatLonToMapcode: Unknown mapcode, exception=", e);
                    }
                });

                // Get the international mapcode.
                final Mapcode mapcodeInternational = MapcodeCodec.encodeToInternational(latDeg, lonDeg);
                mapcodeInternationalAndRectangle = (mapcodeInternational == null) ? null : Tuple.create(
                        mapcodeInternational,
                        MapcodeCodec.decodeToRectangle(mapcodeInternational.getCode())
                );

                // Get the shortest local mapcode.
                Mapcode mapcodeLocal = null;
                if (territory != null) {

                    // A territory was provided, so simply use first.
                    mapcodeLocal = MapcodeCodec.encodeToShortest(latDeg, lonDeg, territory);
                } else {

                    // Get the shortest code.
                    Territory localTerritory = null;
                    for (final Mapcode mapcode : mapcodes) {
                        if (mapcode.getTerritory() != Territory.AAA) {
                            if (localTerritory == null) {

                                // First local territory found. Use a local mapcode, unless another territory is found.
                                localTerritory = mapcode.getTerritory();
                                mapcodeLocal = mapcode;
                            } else {
                                if (localTerritory != mapcode.getTerritory()) {

                                    // Found another local territory; reset local mapcode.
                                    if (mapcode.getCode().length() < mapcodeLocal.getCode().length()) {
                                        mapcodeLocal = mapcode;
                                        localTerritory = mapcode.getTerritory();
                                    }
                                }
                            }
                        }
                    }
                }
                mapcodeLocalAndRectangle = (mapcodeLocal == null) ? null : Tuple.create(
                        mapcodeLocal,
                        MapcodeCodec.decodeToRectangle(mapcodeLocal.getCode(), mapcodeLocal.getTerritory())
                );
            } catch (final UnknownMapcodeException ignored) {

                // The mapcode conversion failed.
                throw new ApiNotFoundException("No mapcode found for lat=" + latDeg + ", lon=" + lonDeg + ", territory=" + territory);
            }

            // Create result body, which is an ApiDTO. The exact type of DTO is still to be determined below.
            final ApiDTO result;
            if (type == null) {

                // No type was supplied, so we need to return the local, international and all mapcodes.
                result = new MapcodesDTO(
                        (mapcodeLocalAndRectangle == null) ? null :
                                createMapcodeDTO(mapcodeLocalAndRectangle, precision, alphabet, includeOffset, includeTerritory,
                                        includeAlphabet, includeRectangle, latDeg, lonDeg),
                        createMapcodeDTO(mapcodeInternationalAndRectangle, precision, alphabet, includeOffset, includeTerritory,
                                includeAlphabet, includeRectangle, latDeg, lonDeg),
                        mapcodesAndRectangles.stream().
                                map(mapcode -> createMapcodeDTO(mapcode, precision, alphabet, includeOffset, includeTerritory,
                                        includeAlphabet, includeRectangle, latDeg, lonDeg)).
                                collect(Collectors.toList()));
            } else {

                // Return only the local, international or all mapcodes.
                switch (type) {
                    case LOCAL: {
                        if (mapcodeLocalAndRectangle == null) {
                            throw new UnknownMapcodeException("No local mapcode for: " +
                                    ((mapcodeInternationalAndRectangle == null) ? null : mapcodeInternationalAndRectangle.getValue1().getCode()));
                        }
                        result = createMapcodeDTO(mapcodeLocalAndRectangle, precision, alphabet, includeOffset, includeTerritory,
                                includeAlphabet, includeRectangle, latDeg, lonDeg);
                        break;
                    }

                    case INTERNATIONAL: {
                        result = createMapcodeDTO(mapcodeInternationalAndRectangle, precision, alphabet, includeOffset, includeTerritory,
                                includeAlphabet, includeRectangle, latDeg, lonDeg);
                        break;
                    }

                    case MAPCODES: {
                        result = new MapcodeListDTO(mapcodesAndRectangles.stream().
                                map(mapcode -> createMapcodeDTO(mapcode, precision, alphabet, includeOffset, includeTerritory,
                                        includeAlphabet, includeRectangle, latDeg, lonDeg)).
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
            metricsCollector.addOneValidLatLonToMapcodeRequest(paramClient);
            response.resume(Response.ok(result).build());

            // The response is already set within this method body.
            return Futures.successful(null);
        });
    }

    @Override
    public void convertLatLonToMapcodeXml(
            final double paramLatDeg,
            final double paramLonDeg,
            @Nullable final String paramType,
            final int paramPrecision,
            @Nullable final String paramTerritory,
            @Nullable final String paramContextMustBeNull,
            @Nullable final String paramAlphabet,
            @Nonnull final String paramInclude,
            @Nonnull final String paramClient,
            @Nonnull final String paramDebug,
            @Suspended @Nonnull final AsyncResponse response)
            throws ApiInvalidFormatException {
        convertLatLonToMapcode(paramLatDeg, paramLonDeg, paramType, paramPrecision, paramTerritory, paramContextMustBeNull,
                paramAlphabet, paramInclude, paramClient, paramDebug, response);
    }

    @Override
    public void convertLatLonToMapcodeJson(
            final double paramLatDeg,
            final double paramLonDeg,
            @Nullable final String paramType,
            final int paramPrecision,
            @Nullable final String paramTerritory,
            @Nullable final String paramContextMustBeNull,
            @Nullable final String paramAlphabet,
            @Nonnull final String paramInclude,
            @Nonnull final String paramClient,
            @Nonnull final String paramDebug,
            @Suspended @Nonnull final AsyncResponse response)
            throws ApiInvalidFormatException {
        convertLatLonToMapcode(paramLatDeg, paramLonDeg, paramType, paramPrecision, paramTerritory, paramContextMustBeNull,
                paramAlphabet, paramInclude, paramClient, paramDebug, response);
    }

    @Override
    public void convertMapcodeToLatLon(
            @Nonnull final AsyncResponse response) throws ApiNotFoundException, ApiInvalidFormatException {

        // This method is forbidden. In REST terms, this would return all world coordinates - intractable.
        processor.process("convertLatLonToMapcode", LOG, response, () -> {
            throw new ApiForbiddenException("Missing URL path parameters: /{mapcode}");
        });
    }

    @Override
    public void convertMapcodeToLatLonXml(
            @Suspended @Nonnull final AsyncResponse response)
            throws ApiNotFoundException, ApiInvalidFormatException {
        convertMapcodeToLatLon(response);
    }

    @Override
    public void convertMapcodeToLatLonJson(
            @Suspended @Nonnull final AsyncResponse response)
            throws ApiNotFoundException, ApiInvalidFormatException {
        convertMapcodeToLatLon(response);
    }

    @Override
    public void convertMapcodeToLatLon(
            @Nonnull final String paramCode,
            @Nullable final String paramContext,
            @Nullable final String paramTerritoryMustBeNull,
            @Nonnull final String paramInclude,
            @Nonnull final String paramClient,
            @Nonnull final String paramAllowLog,
            @Nonnull final AsyncResponse response) throws ApiNotFoundException, ApiInvalidFormatException {
        assert paramCode != null;
        assert response != null;

        processor.process("convertMapcodeToLatLon", LOG, response, () -> {
            // Get debug mode.
            final boolean allowLog = "true".equalsIgnoreCase(paramAllowLog);

            LOG.info("convertMapcodeToLatLon: code={}, territory={}, include={}, client={}, allowLog={}",
                    paramCode, paramContext, paramInclude, paramClient, paramAllowLog);
            metricsCollector.addOneMapcodeToLatLonRequest(paramClient);

            // Prevent 'territory' from inadvertently being specified.
            if (paramTerritoryMustBeNull != null) {
                throw new ApiInvalidFormatException(PARAM_TERRITORY, paramTerritoryMustBeNull, "null");
            }

            // Check include parameter.
            boolean foundIncludeRectangle = false;
            for (final String arg : paramInclude.toUpperCase().split(",")) {
                if (!arg.isEmpty()) {
                    try {
                        final ParamInclude include = ParamInclude.valueOf(arg);
                        foundIncludeRectangle = foundIncludeRectangle || (include == ParamInclude.RECTANGLE);
                    } catch (final IllegalArgumentException ignored) {
                        throw new ApiInvalidFormatException(PARAM_INCLUDE, paramInclude, API_ERROR_VALID_INCLUDES.toLowerCase());
                    }
                }
            }

            // Get the territory from the path (if specified).
            final Territory territoryContext;
            if (paramContext != null) {
                try {
                    // Query parameters are HTML escaped.
                    territoryContext = resolveTerritory(StringEscapeUtils.unescapeHtml4(paramContext), null);
                } catch (final IllegalArgumentException ignored) {
                    throw new ApiInvalidFormatException(PARAM_TERRITORY, paramContext, API_ERROR_VALID_TERRITORY_CODES);
                }

            } else {
                territoryContext = null;
            }

            // Check if the mapcode is correctly formatted.
            if (!Mapcode.isValidMapcodeFormat(paramCode)) {
                throw new ApiInvalidFormatException("mapcode", paramCode, "[XXX] XX.XX[-XX]");
            }

            // Send a trace event with the mapcode and territory.
            if (allowLog) {
                TRACER.eventMapcodeToLatLon(paramCode, territoryContext, UTCTime.now(), paramClient);
            }

            // Create result body (always an ApiDTO).
            final ApiDTO result;
            if (foundIncludeRectangle) {
                try {
                    final Rectangle rectangle = MapcodeCodec.decodeToRectangle(paramCode, territoryContext);
                    result = new RectangleDTO(rectangle);
                } catch (final UnknownMapcodeException ignored) {
                    throw new ApiNotFoundException("No rectangle found for mapcode='" + paramCode + "', context=" + territoryContext);
                }
            } else {
                try {
                    final Point point = MapcodeCodec.decode(paramCode, territoryContext);
                    result = new PointDTO(point.getLatDeg(), point.getLonDeg());
                } catch (final UnknownMapcodeException ignored) {
                    throw new ApiNotFoundException("No location found for mapcode='" + paramCode + "', context=" + territoryContext);
                }
            }

            // Validate the result (internal consistency check).
            result.validate();
            metricsCollector.addOneValidMapcodeToLatLonRequest(paramClient);
            response.resume(Response.ok(result).build());

            // The response is already set within this method body.
            return Futures.successful(null);
        });
    }

    @Override
    public void convertMapcodeToLatLonXml(
            @Nonnull final String paramCode,
            @Nullable final String paramContext,
            @Nullable final String paramTerritoryMustBeNull,
            @Nonnull final String paramInclude,
            @Nonnull final String paramClient,
            @Nonnull final String paramDebug,
            @Suspended @Nonnull final AsyncResponse response)
            throws ApiNotFoundException, ApiInvalidFormatException {
        convertMapcodeToLatLon(paramCode, paramContext, paramTerritoryMustBeNull, paramInclude, paramClient, paramDebug, response);
    }

    @Override
    public void convertMapcodeToLatLonJson(
            @Nonnull final String paramCode,
            @Nullable final String paramContext,
            @Nullable final String paramTerritoryMustBeNull,
            @Nonnull final String paramInclude,
            @Nonnull final String paramClient,
            @Nonnull final String paramDebug,
            @Suspended @Nonnull final AsyncResponse response)
            throws ApiNotFoundException, ApiInvalidFormatException {
        convertMapcodeToLatLon(paramCode, paramContext, paramTerritoryMustBeNull, paramInclude, paramClient, paramDebug, response);
    }

    @Override
    public void getTerritories(
            final int offset,
            final int count,
            @Nonnull final String paramClient,
            @Nonnull final String paramAllowLog,
            @Nonnull final AsyncResponse response) throws ApiIntegerOutOfRangeException {
        assert response != null;

        processor.process("getTerritories", LOG, response, () -> {
            LOG.info("getTerritories: client={}, allowLog={}", paramClient, paramAllowLog);
            metricsCollector.addOneTerritoryRequest(paramClient);

            // Check value of count.
            if (count < 0) {
                throw new ApiIntegerOutOfRangeException(PARAM_COUNT, count, 0, Integer.MAX_VALUE);
            }
            assert count >= 0;

            // Create the response and validate it.
            final int nrTerritories = ALL_TERRITORY_DTO.size();
            final int fromIndex = (offset < 0) ? Math.max(0, nrTerritories + offset) : Math.min(nrTerritories, offset);
            final int toIndex = Math.min(nrTerritories, fromIndex + count);
            final TerritoryListDTO territoryList = new TerritoryListDTO(ALL_TERRITORY_DTO.subList(fromIndex, toIndex));
            final TerritoriesDTO result = new TerritoriesDTO(ALL_TERRITORY_DTO.size(), territoryList);

            // Validate the result (internal consistency check).
            result.validate();
            response.resume(Response.ok(result).build());

            // The response is already set within this method body.
            return Futures.successful(null);
        });
    }

    @Override
    public void getTerritoriesXml(
            final int offset,
            final int count,
            @Nonnull final String paramClient,
            @Nonnull final String paramAllowLog,
            @Suspended @Nonnull final AsyncResponse response)
            throws ApiIntegerOutOfRangeException {
        getTerritories(offset, count, paramClient, paramAllowLog, response);
    }

    @Override
    public void getTerritoriesJson(
            final int offset,
            final int count,
            @Nonnull final String paramClient,
            @Nonnull final String paramAllowLog,
            @Suspended @Nonnull final AsyncResponse response)
            throws ApiIntegerOutOfRangeException {
        getTerritories(offset, count, paramClient, paramAllowLog, response);
    }

    @Override
    public void getTerritory(
            @Nonnull final String paramTerritory,
            @Nullable final String paramContext,
            @Nonnull final String paramClient,
            @Nonnull final String paramAllowLog,
            @Nonnull final AsyncResponse response) throws ApiInvalidFormatException {
        assert paramTerritory != null;
        assert response != null;

        processor.process("getTerritory", LOG, response, () -> {
            LOG.info("getTerritory: territory={}, context={}, client={}, allowLog={}", paramTerritory, paramContext, paramClient, paramAllowLog);
            metricsCollector.addOneTerritoryRequest(paramClient);

            // Get the territory from the URL.
            final Territory territory;
            try {
                territory = resolveTerritory(paramTerritory, StringEscapeUtils.unescapeHtml4(paramContext));
            } catch (final IllegalArgumentException ignored) {
                throw new ApiInvalidFormatException(PARAM_TERRITORY, paramTerritory, API_ERROR_VALID_TERRITORY_CODES);
            }

            // Return the right territory information.
            final Territory parentTerritory = territory.getParentTerritory();
            final TerritoryDTO result = new TerritoryDTO(
                    territory.toString(),
                    territory.toAlphaCode(AlphaCodeFormat.MINIMAL_UNAMBIGUOUS),
                    territory.toAlphaCode(AlphaCodeFormat.MINIMAL),
                    territory.getFullName(),
                    (parentTerritory == null) ? null : parentTerritory.toString(),
                    territory.getAliases(),
                    territory.getFullNameAliases(),
                    territory.getAlphabets()
            );

            // Validate the result (internal consistency check).
            result.validate();
            response.resume(Response.ok(result).build());

            // The response is already set within this method body.
            return Futures.successful(null);
        });
    }

    @Override
    public void getTerritoryXml(
            @Nonnull final String paramTerritory,
            @Nullable final String paramContext,
            @Nonnull final String paramClient,
            @Nonnull final String paramAllowLog,
            @Suspended @Nonnull final AsyncResponse response)
            throws ApiInvalidFormatException {
        getTerritory(paramTerritory, paramContext, paramClient, paramAllowLog, response);
    }

    @Override
    public void getTerritoryJson(
            @Nonnull final String paramTerritory,
            @Nullable final String paramContext,
            @Nonnull final String paramClient,
            @Nonnull final String paramAllowLog,
            @Suspended @Nonnull final AsyncResponse response)
            throws ApiInvalidFormatException {
        getTerritory(paramTerritory, paramContext, paramClient, paramAllowLog, response);
    }

    @Override
    public void getAlphabets(
            final int offset,
            final int count,
            @Nonnull final String paramClient,
            @Nonnull final String paramAllowLog,
            @Nonnull final AsyncResponse response) throws ApiIntegerOutOfRangeException {
        assert response != null;

        processor.process("getAlphabets", LOG, response, () -> {
            LOG.info("getAlphabets: clien={}, allowLog={}", paramClient, paramAllowLog);
            metricsCollector.addOneAlphabetRequest(paramClient);

            // Check value of count.
            if (count < 0) {
                throw new ApiIntegerOutOfRangeException(PARAM_COUNT, count, 0, Integer.MAX_VALUE);
            }
            assert count >= 0;

            // Create the response and validate it.
            final int nrAlphabets = ALL_ALPHABET_DTO.size();
            final int fromIndex = (offset < 0) ? Math.max(0, nrAlphabets + offset) : Math.min(nrAlphabets, offset);
            final int toIndex = Math.min(nrAlphabets, fromIndex + count);
            final AlphabetListDTO alphabetList = new AlphabetListDTO(ALL_ALPHABET_DTO.subList(fromIndex, toIndex));
            final AlphabetsDTO result = new AlphabetsDTO(ALL_ALPHABET_DTO.size(), alphabetList);

            // Validate the result (internal consistency check).
            result.validate();
            response.resume(Response.ok(result).build());

            // The response is already set within this method body.
            return Futures.successful(null);
        });
    }

    @Override
    public void getAlphabetsXml(
            final int offset,
            final int count,
            @Nonnull final String paramClient,
            @Nonnull final String paramAllowLog,
            @Suspended @Nonnull final AsyncResponse response)
            throws ApiIntegerOutOfRangeException {
        getAlphabets(offset, count, paramClient, paramAllowLog, response);
    }

    @Override
    public void getAlphabetsJson(
            final int offset,
            final int count,
            @Nonnull final String paramClient,
            @Nonnull final String paramAllowLog,
            @Suspended @Nonnull final AsyncResponse response)
            throws ApiIntegerOutOfRangeException {
        getAlphabets(offset, count, paramClient, paramAllowLog, response);
    }

    @Override
    public void getAlphabet(
            @Nonnull final String paramAlphabet,
            @Nonnull final String paramClient,
            @Nonnull final String paramAllowLog,
            @Nonnull final AsyncResponse response) throws ApiInvalidFormatException {
        assert paramAlphabet != null;
        assert response != null;

        processor.process("getAlphabet", LOG, response, () -> {

            LOG.info("getAlphabet: alphabet={}, client={}, allowLog={}", paramAlphabet, paramClient, paramAllowLog);
            metricsCollector.addOneAlphabetRequest(paramClient);

            // Get the territory from the URL.
            final Alphabet alphabet;
            try {
                alphabet = Alphabet.fromString(paramAlphabet);
            } catch (final IllegalArgumentException ignored) {
                throw new ApiInvalidFormatException("alphabet", paramAlphabet, API_ERROR_VALID_ALPHABET_CODES);
            }

            // Return the right territory information.
            final AlphabetDTO result = new AlphabetDTO(alphabet);

            // Validate the result (internal consistency check).
            result.validate();
            response.resume(Response.ok(result).build());

            // The response is already set within this method body.
            return Futures.successful(null);
        });
    }

    @Override
    public void getAlphabetXml(
            @Nonnull final String paramAlphabet,
            @Nonnull final String paramClient,
            @Nonnull final String paramAllowLog,
            @Suspended @Nonnull final AsyncResponse response)
            throws ApiInvalidFormatException {
        getAlphabet(paramAlphabet, paramClient, paramAllowLog, response);
    }

    @Override
    public void getAlphabetJson(
            @Nonnull final String paramAlphabet,
            @Nonnull final String paramClient,
            @Nonnull final String paramAllowLog,
            @Suspended @Nonnull final AsyncResponse response)
            throws ApiInvalidFormatException {
        getAlphabet(paramAlphabet, paramClient, paramAllowLog, response);
    }

    @Nonnull
    private static Territory resolveTerritory(@Nonnull final String paramTerritory, @Nullable final String paramParent) {
        Territory parentTerritory;
        if (paramParent != null) {

            // Try to use the parent territory, if available.
            Territory context;
            try {
                final String cleaned = paramParent.replace('-', '_').toUpperCase();
                context = Territory.fromString(cleaned);
                if (context.getParentTerritory() != null) {
                    context = context.getParentTerritory();
                }
            } catch (final IllegalArgumentException ignored) {

                // Check if it was an alias.
                context = getTerritoryAlias(paramParent);
                if (context == null) {
                    throw new ApiInvalidFormatException("parent", paramParent, API_ERROR_VALID_TERRITORY_CODES);
                }
            }

            // Use the resolved context.
            try {
                parentTerritory = Territory.valueOf(context.toString());
            } catch (final IllegalArgumentException ignored) {

                // Explicitly remove parent context.
                parentTerritory = null;
            }
        } else {
            parentTerritory = null;
        }

        // Try with parent if available.
        if (parentTerritory != null) {
            try {
                return Territory.fromString(paramTerritory.toUpperCase(), parentTerritory);
            } catch (final UnknownTerritoryException ignored) {
                // If using the parent fails, try without the parent.
            }
        }
        return Territory.fromString(paramTerritory.toUpperCase());
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
    private static MapcodeDTO createMapcodeDTO(@Nonnull final Tuple<Mapcode, Rectangle> mapcodeAndRectangle, final int precision,
                                               @Nullable final Alphabet alphabet, final boolean includeOffset,
                                               final boolean includeTerritory, final boolean includeAlphabet, final boolean includeRectangle,
                                               final double latDeg, final double lonDeg) {
        final Mapcode mapcode = mapcodeAndRectangle.getValue1();
        final Rectangle rectangle = mapcodeAndRectangle.getValue2();
        final String code = mapcode.getCode(precision);
        final String codeInAlphabet = mapcode.getCode(precision, alphabet);
        final String territory = mapcode.getTerritory().toString();
        final String territoryInAlphabet = mapcode.getTerritory().toString(alphabet);
        final boolean includeOrLocal = includeTerritory || (mapcode.getTerritory() != Territory.AAA);
        return new MapcodeDTO(
                code,
                includeAlphabet ? codeInAlphabet : (codeInAlphabet.equals(code) ? null : codeInAlphabet),
                includeOrLocal ? territory : null,
                includeOrLocal ? (includeAlphabet ? territoryInAlphabet : (territoryInAlphabet.equals(territory) ? null : territoryInAlphabet)) : null,
                includeOffset ? offsetFromLatLonInMeters(latDeg, lonDeg, mapcode, precision) : null,
                includeRectangle ? new RectangleDTO(rectangle) : null);
    }

    private static double offsetFromLatLonInMeters(
            final double latDeg,
            final double lonDeg,
            @Nonnull final Mapcode mapcode,
            final int precision) {
        assert mapcode != null;
        final GeoPoint position = new GeoPoint(latDeg, lonDeg);
        try {
            final Point point = MapcodeCodec.decode(mapcode.getCode(precision), mapcode.getTerritory());
            final GeoPoint center = new GeoPoint(point.getLatDeg(), point.getLonDeg());
            final double distanceMeters = Geo.distanceInMeters(position, center);
            return Math.round(distanceMeters * 1.0e6) / 1.0e6;
        } catch (final UnknownMapcodeException ignore) {
            // Simply ignore.
            return 0.0;
        }
    }

    /**
     * This interface defines a Tracer interface for mapcode service events.
     */
    public interface Tracer extends Traceable {

        // A request to translate a lat/lon to a mapcode is made.
        void eventLatLonToMapcode(double latDeg, double lonDeg, @Nullable Territory territory,
                                  int precision, @Nullable String type, @Nullable String alphabet,
                                  @Nullable String include, @Nonnull DateTime now, @Nullable final String client);

        // A request to translate a mapcode to a lat/lon is made.
        void eventMapcodeToLatLon(@Nonnull String code, @Nullable Territory territory, @Nonnull DateTime now,
                                  @Nullable final String client);

        // A request to translate a lat/lon to a mapcode is made.
        @Deprecated
        void eventLatLonToMapcode(double latDeg, double lonDeg, @Nullable Territory territory,
                                  int precision, @Nullable String type, @Nullable String alphabet,
                                  @Nullable String include, @Nonnull DateTime now);

        // A request to translate a mapcode to a lat/lon is made.
        @Deprecated
        void eventMapcodeToLatLon(@Nonnull String code, @Nullable Territory territory, @Nonnull DateTime now);
    }
}
