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
import com.mapcode.Territory.AlphaCodeFormat;
import com.mapcode.services.ApiConstants;
import com.mapcode.services.MapcodeResource;
import com.mapcode.services.SystemMetricsCollector;
import com.mapcode.services.dto.*;
import com.tomtom.speedtools.apivalidation.ApiDTO;
import com.tomtom.speedtools.apivalidation.exceptions.ApiForbiddenException;
import com.tomtom.speedtools.apivalidation.exceptions.ApiIntegerOutOfRangeException;
import com.tomtom.speedtools.apivalidation.exceptions.ApiInvalidFormatException;
import com.tomtom.speedtools.apivalidation.exceptions.ApiNotFoundException;
import com.tomtom.speedtools.geometry.Geo;
import com.tomtom.speedtools.geometry.GeoPoint;
import com.tomtom.speedtools.rest.ResourceProcessor;
import com.tomtom.speedtools.time.UTCTime;
import com.tomtom.speedtools.tracer.Traceable;
import com.tomtom.speedtools.tracer.TracerFactory;
import com.tomtom.speedtools.utils.MathUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jboss.resteasy.spi.AsynchronousResponse;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
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
    private final SystemMetricsCollector metricsCollector;

    private static final String API_ERROR_VALID_TERRITORY_CODES = Joiner.on('|').join(Arrays.asList(Territory.values()).stream().
            map(x -> (x.toString())).collect(Collectors.toList()));

    private static final String API_ERROR_VALID_ALPHABET_CODES = Joiner.on('|').join(Arrays.asList(Alphabet.values()).stream().
            map(x -> (x.toString())).collect(Collectors.toList()));

    private static final String API_ERROR_VALID_TYPES = Joiner.on('|').join(Arrays.asList(ParamType.values()).stream().
            map(x -> x).collect(Collectors.toList()));

    private static final String API_ERROR_VALID_INCLUDES = Joiner.on('|').join(Arrays.asList(ParamInclude.values()).stream().
            map(x -> x).collect(Collectors.toList()));

    private static final List<TerritoryDTO> ALL_TERRITORY_DTO = Arrays.asList(Territory.values()).stream().
            map(x -> {
                final Territory parentTerritory = x.getParentTerritory();
                return new TerritoryDTO(
                        x.toString(),
                        x.toAlphaCode(AlphaCodeFormat.MINIMAL_UNAMBIGUOUS),
                        x.toAlphaCode(AlphaCodeFormat.MINIMAL),
                        x.getFullName(),
                        (parentTerritory == null) ? null : parentTerritory.toString(),
                        x.getAliases(),
                        x.getFullNameAliases());
            }).
            collect(Collectors.toList());

    private static final List<AlphabetDTO> ALL_ALPHABET_DTO = Arrays.asList(Alphabet.values()).stream().
            map(x -> new AlphabetDTO(x.name())).
            collect(Collectors.toList());

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
            @Nonnull final HttpServletRequest httpServletRequest,
            @Nonnull final AsynchronousResponse response) throws ApiInvalidFormatException {

        // This method is forbidden. In REST terms, this should return ALL potential mapcodes - intractable.
        throw new ApiForbiddenException("Missing URL path parameters: /{lat,lon}/{" + API_ERROR_VALID_TYPES.toLowerCase() + '}');
    }

    @Override
    public void convertLatLonToMapcode(
            final double paramLatDeg,
            final double paramLonDeg,
            final int paramPrecision,
            @Nullable final String paramTerritory,
            @Nullable final String paramAlphabet,
            @Nonnull final String paramInclude,
            @Context @Nonnull final HttpServletRequest httpServletRequest,
            @Nonnull final AsynchronousResponse response) throws ApiInvalidFormatException {
        convertLatLonToMapcode(paramLatDeg, paramLonDeg, null, paramPrecision, paramTerritory, paramAlphabet,
                paramInclude, httpServletRequest, response);
    }

    @Override
    public void convertLatLonToMapcode(
            final double paramLatDeg,
            final double paramLonDeg,
            @Nullable final String paramType,
            final int paramPrecision,
            @Nullable final String paramTerritory,
            @Nullable final String paramAlphabet,
            @Nonnull final String paramInclude,
            @Context @Nonnull final HttpServletRequest httpServletRequest,
            @Nonnull final AsynchronousResponse response) throws ApiInvalidFormatException {
        assert response != null;

        processor.process("convertLatLonToMapcode", LOG, response, () -> {
            final String clientIp = getClientIp(httpServletRequest);
            LOG.info("convertLatLonToMapcode: lat={}, lon={}, precision={}, type={}, context={}, alphabet={}, include={}, ip={}",
                    paramLatDeg, paramLonDeg, paramPrecision, paramType, paramTerritory, paramAlphabet, paramInclude, clientIp);
            metricsCollector.addOneLatLonToMapcodeRequest();

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

            // Check include.
            boolean foundIncludeOffset = false;
            boolean foundIncludeTerritory = false;
            boolean foundIncludeAlphabet = false;
            for (final String arg : paramInclude.toUpperCase().split(",")) {
                if (!arg.isEmpty()) {
                    try {
                        final ParamInclude include = ParamInclude.valueOf(arg);
                        foundIncludeOffset = foundIncludeOffset || (include == ParamInclude.OFFSET);
                        foundIncludeTerritory = foundIncludeTerritory || (include == ParamInclude.TERRITORY);
                        foundIncludeAlphabet = foundIncludeAlphabet || (include == ParamInclude.ALPHABET);
                    } catch (final IllegalArgumentException ignored) {
                        throw new ApiInvalidFormatException(PARAM_INCLUDE, paramInclude, API_ERROR_VALID_INCLUDES.toLowerCase());
                    }
                }
            }

            // Determine whether include=offset and territory=xxx were supplied as URL parameters.
            final boolean includeOffset = foundIncludeOffset;
            final boolean includeTerritory = foundIncludeTerritory;
            final boolean includeAlphabet = foundIncludeAlphabet;

            // Send a trace event with the lat/lon and other parameters.
            TRACER.eventLatLonToMapcode(getClientIp(httpServletRequest), latDeg, lonDeg, territory, precision, paramType,
                    paramAlphabet, paramInclude, UTCTime.now());

            try {

                /**
                 * First get all mapcodes. This is not the most efficient implementation, as we encode the
                 * lat/lon 3 times, but unless there is a measured, significant performance issue in real life,
                 * this implementation is good enough for now (and pretty straightforward).
                 */

                // Get all mapcodes.
                final List<Mapcode> mapcodesAll;
                mapcodesAll = MapcodeCodec.encode(latDeg, lonDeg, territory);

                // Get the international mapcode.
                final Mapcode mapcodeInternational = MapcodeCodec.encodeToInternational(latDeg, lonDeg);

                // Get the shortest local mapcode.
                boolean reasonMultipleTerritories = false;
                Mapcode mapcodeLocal = null;
                if (territory != null) {

                    // A territory was provided, so simply use first.
                    mapcodeLocal = MapcodeCodec.encodeToShortest(latDeg, lonDeg, territory);
                } else {
                    Territory localTerritory = null;
                    for (final Mapcode mapcode : mapcodesAll) {
                        if (mapcode.getTerritory() != Territory.AAA) {
                            if (localTerritory == null) {

                                // First local territory found. Use a local mapcode, unless another territory is found/
                                localTerritory = mapcode.getTerritory();
                                mapcodeLocal = mapcode;
                            } else {
                                if (localTerritory != mapcode.getTerritory()) {
                                    // Found another local territory; reset local mapcode.
                                    mapcodeLocal = null;
                                    reasonMultipleTerritories = true;
                                    break;
                                }
                            }
                        }
                    }
                }

                // Create result body, which is an ApiDTO. The exact type of DTO is still to be determined below.
                final ApiDTO result;
                if (type == null) {

                    // No type was supplied, so we need to return the local, international and all mapcodes.
                    result = new MapcodesDTO(
                            (mapcodeLocal == null) ? null :
                                    getMapcodeDTO(mapcodeLocal, precision, alphabet, includeOffset, includeTerritory, includeAlphabet,
                                            latDeg, lonDeg),
                            getMapcodeDTO(mapcodeInternational, precision, alphabet, includeOffset, includeTerritory, includeAlphabet,
                                    latDeg, lonDeg),
                            mapcodesAll.stream().
                                    map(mapcode -> getMapcodeDTO(mapcode, precision, alphabet, includeOffset, includeTerritory,
                                            includeAlphabet, latDeg, lonDeg)).
                                    collect(Collectors.toList()));
                } else {

                    // Return only the local, international or all mapcodes.
                    switch (type) {
                        case LOCAL: {
                            if (mapcodeLocal == null) {
                                throw new ApiNotFoundException((reasonMultipleTerritories ?
                                        "Local mapcodes for multiple territories exist" :
                                        "Only an international mapcode exists") +
                                        " for (" + latDeg + ", " + lonDeg + ')');
                            }
                            result = getMapcodeDTO(mapcodeLocal, precision, alphabet, includeOffset, includeTerritory, includeAlphabet,
                                    latDeg, lonDeg);
                            break;
                        }

                        case INTERNATIONAL: {
                            result = getMapcodeDTO(mapcodeInternational, precision, alphabet, includeOffset, includeTerritory, includeAlphabet,
                                    latDeg, lonDeg);
                            break;
                        }

                        case MAPCODES: {
                            result = new MapcodeListDTO(mapcodesAll.stream().
                                    map(mapcode -> getMapcodeDTO(mapcode, precision, alphabet, includeOffset, includeTerritory, includeAlphabet,
                                            latDeg, lonDeg)).
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
                metricsCollector.addOneValidLatLonToMapcodeRequest();
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
    public void convertMapcodeToLatLon(
            @Context @Nonnull final HttpServletRequest httpServletRequest,
            @Nonnull final AsynchronousResponse response) throws ApiNotFoundException, ApiInvalidFormatException {

        // This method is forbidden. In REST terms, this would return all world coordinates - intractable.
        throw new ApiForbiddenException("Missing URL path parameters: /{mapcode}");
    }

    @Override
    public void convertMapcodeToLatLon(
            @Nonnull final String paramCode,
            @Nullable final String paramContext,
            @Context @Nonnull final HttpServletRequest httpServletRequest,
            @Nonnull final AsynchronousResponse response) throws ApiNotFoundException, ApiInvalidFormatException {
        assert paramCode != null;
        assert response != null;

        processor.process("convertMapcodeToLatLon", LOG, response, () -> {
            final String clientIp = getClientIp(httpServletRequest);
            LOG.info("convertMapcodeToLatLon: code={}, territory={}, ip={}", paramCode, paramContext, clientIp);
            metricsCollector.addOneMapcodeToLatLonRequest();

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
            TRACER.eventMapcodeToLatLon(clientIp, paramCode, territoryContext, UTCTime.now());

            // Create result body (always an ApiDTO).
            final ApiDTO result;
            try {

                // Decode the actual mapcode.
                final Point point;
                point = MapcodeCodec.decode(paramCode, territoryContext);
                result = new PointDTO(point.getLatDeg(), point.getLonDeg());
            } catch (final UnknownMapcodeException ignored) {
                throw new ApiNotFoundException("No mapcode found for mapcode='" + paramCode + "', context=" + territoryContext);
            }

            // Validate the result (internal consistency check).
            result.validate();
            metricsCollector.addOneValidMapcodeToLatLonRequest();
            response.setResponse(Response.ok(result).build());

            // The response is already set within this method body.
            return Futures.successful(null);
        });
    }

    @Override
    public void getTerritories(
            final int offset,
            final int count,
            @Context @Nonnull final HttpServletRequest httpServletRequest,
            @Nonnull final AsynchronousResponse response) throws ApiIntegerOutOfRangeException {
        assert response != null;

        processor.process("getTerritories", LOG, response, () -> {
            final String clientIp = getClientIp(httpServletRequest);
            LOG.info("getTerritories: ip={}", clientIp);

            // Check value of count.
            if (count < 0) {
                throw new ApiIntegerOutOfRangeException(PARAM_COUNT, count, 0, Integer.MAX_VALUE);
            }
            assert count >= 0;

            // Create the response and validate it.
            final int nrTerritories = ALL_TERRITORY_DTO.size();
            final int fromIndex = (offset < 0) ? Math.max(0, nrTerritories + offset) : Math.min(nrTerritories, offset);
            final int toIndex = Math.min(nrTerritories, fromIndex + count);
            final TerritoriesDTO result = new TerritoriesDTO(ALL_TERRITORY_DTO.subList(fromIndex, toIndex));

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
            @Context @Nonnull final HttpServletRequest httpServletRequest,
            @Nonnull final AsynchronousResponse response) throws ApiInvalidFormatException {
        assert paramTerritory != null;
        assert response != null;

        processor.process("getTerritory", LOG, response, () -> {
            final String clientIp = getClientIp(httpServletRequest);
            LOG.info("getTerritory: territory={}, context={}, ip={}", paramTerritory, paramContext, clientIp);

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
                    territory.getFullNameAliases()
            );

            // Validate the result (internal consistency check).
            result.validate();
            response.setResponse(Response.ok(result).build());

            // The response is already set within this method body.
            return Futures.successful(null);
        });
    }

    @Override
    public void getAlphabets(
            final int offset,
            final int count,
            @Context @Nonnull final HttpServletRequest httpServletRequest,
            @Nonnull final AsynchronousResponse response) throws ApiIntegerOutOfRangeException {
        assert response != null;

        processor.process("getAlphabets", LOG, response, () -> {
            final String clientIp = getClientIp(httpServletRequest);
            LOG.info("getAlphabets: ip={}", clientIp);

            // Check value of count.
            if (count < 0) {
                throw new ApiIntegerOutOfRangeException(PARAM_COUNT, count, 0, Integer.MAX_VALUE);
            }
            assert count >= 0;

            // Create the response and validate it.
            final int nrAlphabets = ALL_ALPHABET_DTO.size();
            final int fromIndex = (offset < 0) ? Math.max(0, nrAlphabets + offset) : Math.min(nrAlphabets, offset);
            final int toIndex = Math.min(nrAlphabets, fromIndex + count);
            final AlphabetsDTO result = new AlphabetsDTO(ALL_ALPHABET_DTO.subList(fromIndex, toIndex));

            // Validate the result (internal consistency check).
            result.validate();
            response.setResponse(Response.ok(result).build());

            // The response is already set within this method body.
            return Futures.successful(null);
        });
    }

    @Override
    public void getAlphabet(
            @Nonnull final String paramAlphabet,
            @Context @Nonnull final HttpServletRequest httpServletRequest,
            @Nonnull final AsynchronousResponse response) throws ApiInvalidFormatException {
        assert paramAlphabet != null;
        assert response != null;

        processor.process("getAlphabet", LOG, response, () -> {
            final String clientIp = getClientIp(httpServletRequest);
            LOG.info("getAlphabet: alphabet={}, ip={}", paramAlphabet, clientIp);

            // Get the territory from the URL.
            final Alphabet alphabet;
            try {
                alphabet = Alphabet.fromString(paramAlphabet);
            } catch (final IllegalArgumentException ignored) {
                throw new ApiInvalidFormatException("alphabet", paramAlphabet, API_ERROR_VALID_ALPHABET_CODES);
            }

            // Return the right territory information.
            final AlphabetDTO result = new AlphabetDTO(alphabet.name());

            // Validate the result (internal consistency check).
            result.validate();
            response.setResponse(Response.ok(result).build());

            // The response is already set within this method body.
            return Futures.successful(null);
        });
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
    private static MapcodeDTO getMapcodeDTO(@Nonnull final Mapcode mapcode, final int precision,
                                            @Nullable final Alphabet alphabet, final boolean includeOffset,
                                            final boolean includeTerritory, final boolean includeAlphabet,
                                            final double latDeg, final double lonDeg) {
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
            final Point point = MapcodeCodec.decode(mapcode.getCode(precision), mapcode.getTerritory());
            final GeoPoint center = new GeoPoint(point.getLatDeg(), point.getLonDeg());
            final double distanceMeters = Geo.distanceInMeters(position, center);
            return Math.round(distanceMeters * 1.0e6) / 1.0e6;
        } catch (final UnknownMapcodeException ignore) {
            // Simply ignore.
            return 0.0;
        }
    }

    @Nonnull
    public static String getClientIp(@Nonnull final HttpServletRequest httpServletRequest) {
        final String unknown = "unknown";
        String ip = httpServletRequest.getHeader("X-Forwarded-For");
        if ((ip == null) || ip.isEmpty() || unknown.equalsIgnoreCase(ip)) {
            ip = httpServletRequest.getHeader("Proxy-Client-IP");
        }
        if ((ip == null) || ip.isEmpty() || unknown.equalsIgnoreCase(ip)) {
            ip = httpServletRequest.getHeader("WL-Proxy-Client-IP");
        }
        if ((ip == null) || ip.isEmpty() || unknown.equalsIgnoreCase(ip)) {
            ip = httpServletRequest.getHeader("HTTP_CLIENT_IP");
        }
        if ((ip == null) || ip.isEmpty() || unknown.equalsIgnoreCase(ip)) {
            ip = httpServletRequest.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if ((ip == null) || ip.isEmpty() || unknown.equalsIgnoreCase(ip)) {
            ip = httpServletRequest.getRemoteAddr();
        }
        return ip;
    }

    /**
     * This interface defines a Tracer interface for mapcode service events.
     */
    public interface Tracer extends Traceable {

        // A request to translate a lat/lon to a mapcode is made.
        void eventLatLonToMapcode(@Nonnull String clientIp, double latDeg, double lonDeg, @Nullable Territory territory,
                                  int precision, @Nullable String type, @Nullable String alphabet,
                                  @Nullable String include, @Nonnull DateTime now);

        // A request to translate a mapcode to a lat/lon is made.
        void eventMapcodeToLatLon(@Nonnull String clientIp, @Nonnull String code, @Nullable Territory territory, @Nonnull DateTime now);
    }
}

