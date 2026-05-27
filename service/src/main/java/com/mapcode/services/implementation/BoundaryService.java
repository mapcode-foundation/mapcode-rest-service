/*
 * Copyright (C) 2016-2026, Stichting Mapcode Foundation (http://www.mapcode.com)
 */
package com.mapcode.services.implementation;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.index.strtree.STRtree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wololo.flatgeobuf.GeometryConversions;
import org.wololo.flatgeobuf.HeaderMeta;
import org.wololo.flatgeobuf.PackedRTree;
import org.wololo.flatgeobuf.generated.ColumnType;
import org.wololo.flatgeobuf.generated.Feature;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Loads a FlatGeobuf borders file at construction time and answers point-in-polygon queries.
 */
public class BoundaryService {

    private static final Logger LOG = LoggerFactory.getLogger(BoundaryService.class);
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    private static final Point PRIMING_POINT = GEOMETRY_FACTORY.createPoint(new Coordinate(0.0, 0.0));

    private final STRtree index;
    private final List<IndexedEntry> entries = new ArrayList<>();

    public BoundaryService(@Nonnull final String bordersFilePath) {
        final Path path = Paths.get(bordersFilePath);
        if (!Files.isReadable(path)) {
            throw new IllegalStateException("Borders file not readable: " + path);
        }
        this.index = new STRtree();
        final int loaded;
        try {
            loaded = loadFeatures(Files.readAllBytes(path));
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to load borders file: " + path, e);
        }
        index.build();
        primePreparedGeometries();
        LOG.info("BoundaryService: loaded {} polygons from {}", loaded, path);
    }

    /**
     * Loads the borders data from an {@link InputStream}. The stream is fully consumed and
     * closed by this constructor. {@code sourceDescription} is used only for log and error messages.
     */
    public BoundaryService(@Nonnull final InputStream stream,
                           @Nonnull final String sourceDescription) {
        this.index = new STRtree();
        final int loaded;
        try {
            loaded = loadFeatures(toByteArray(stream));
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to load borders from " + sourceDescription, e);
        }
        index.build();
        primePreparedGeometries();
        LOG.info("BoundaryService: loaded {} polygons from {}", loaded, sourceDescription);
    }

    /**
     * Force the lazy segment-tree index inside each PreparedGeometry to be built at startup,
     * so the first user request does not pay that one-time cost. Result of the contains() call
     * itself is irrelevant — we only need the side-effect of priming the internal locator.
     */
    private void primePreparedGeometries() {
        for (final IndexedEntry entry : entries) {
            entry.prepared.contains(PRIMING_POINT);
        }
    }

    private static byte[] toByteArray(@Nonnull final InputStream stream) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final byte[] buf = new byte[8192];
        int n;
        while ((n = stream.read(buf)) != -1) {
            baos.write(buf, 0, n);
        }
        return baos.toByteArray();
    }

    private int loadFeatures(@Nonnull final byte[] bytes) throws IOException {
        final ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

        // Parse header — HeaderMeta.read(ByteBuffer) advances buf.position() past the header.
        final HeaderMeta header = HeaderMeta.read(buf);

        // Skip the spatial index, if present (indexNodeSize == 0 means no index).
        if (header.indexNodeSize > 0 && header.featuresCount > 0) {
            if (header.featuresCount > Integer.MAX_VALUE) {
                throw new IllegalStateException("FlatGeobuf feature count too large: " + header.featuresCount);
            }
            final long indexSize = PackedRTree.calcSize((int) header.featuresCount, header.indexNodeSize);
            if (indexSize > Integer.MAX_VALUE) {
                throw new IllegalStateException("FlatGeobuf spatial index too large: " + indexSize + " bytes");
            }
            buf.position(buf.position() + (int) indexSize);
        }

        int count = 0;
        // Read features sequentially; each is size-prefixed (4-byte little-endian length).
        while (buf.remaining() >= 4) {
            final int featureSize = buf.getInt();
            if (featureSize <= 0 || featureSize > buf.remaining()) {
                break;
            }
            // Copy feature bytes into a fresh array so position=0 for FlatBuffers root-table lookup.
            final byte[] featureBytes = new byte[featureSize];
            buf.get(featureBytes);
            final ByteBuffer featureBuf = ByteBuffer.wrap(featureBytes).order(ByteOrder.LITTLE_ENDIAN);

            final Feature feature = Feature.getRootAsFeature(featureBuf);

            // Deserialize geometry.
            if (feature.geometry() == null) {
                continue;
            }
            final Geometry geometry = GeometryConversions.deserialize(
                    feature.geometry(), header.geometryType);
            if (geometry == null || geometry.isEmpty()) {
                continue;
            }

            // Decode properties blob.
            final ByteBuffer props = feature.propertiesAsByteBuffer();
            if (props == null) {
                continue;
            }
            final String alphaCode = readStringProp(props, header, "alphaCode");
            if (alphaCode == null) {
                continue;
            }
            String parentAlphaCode = readStringProp(props, header, "parentAlphaCode");
            if (parentAlphaCode != null && parentAlphaCode.isEmpty()) {
                parentAlphaCode = null;
            }
            final Integer adminLevel = readIntProp(props, header, "adminLevel");
            if (adminLevel == null) {
                continue;
            }
            final Double area = readDoubleProp(props, header, "area");
            if (area == null) {
                continue;
            }

            final PreparedGeometry prepared = PreparedGeometryFactory.prepare(geometry);
            final IndexedEntry entry = new IndexedEntry(prepared, alphaCode, parentAlphaCode,
                    adminLevel, area);
            index.insert(geometry.getEnvelopeInternal(), entry);
            entries.add(entry);
            count++;
        }
        return count;
    }

    /**
     * Reads a named property from the FlatGeobuf properties binary blob and returns it cast to
     * {@code type}, or {@code null} if the column is absent or has an incompatible type.
     * Properties encoding: repeated [ uint16 column-index | type-specific bytes ].
     */
    @Nullable
    private static <T> T readProp(@Nonnull final ByteBuffer props,
                                  @Nonnull final HeaderMeta header,
                                  @Nonnull final String name,
                                  @Nonnull final Class<T> type) {
        final ByteBuffer p = props.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        while (p.remaining() >= 2) {
            final int colIdx = p.getShort() & 0xFFFF;
            if (colIdx >= header.columns.size()) {
                break;
            }
            final byte colType = header.columns.get(colIdx).type;
            final String colName = header.columns.get(colIdx).name;
            final Object value = readTypedValue(p, colType);
            if (name.equals(colName) && type.isInstance(value)) {
                return type.cast(value);
            }
        }
        return null;
    }

    @Nullable
    private static String readStringProp(@Nonnull final ByteBuffer props,
                                         @Nonnull final HeaderMeta header,
                                         @Nonnull final String name) {
        return readProp(props, header, name, String.class);
    }

    @Nullable
    private static Integer readIntProp(@Nonnull final ByteBuffer props,
                                       @Nonnull final HeaderMeta header,
                                       @Nonnull final String name) {
        // FlatGeobuf ColumnType.Int (int32): readTypedValue returns Integer.
        final Integer asInt = readProp(props, header, name, Integer.class);
        if (asInt != null) {
            return asInt;
        }
        // FlatGeobuf ColumnType.Long (int64): GDAL/geopandas uses this for pandas int64 columns.
        final Long asLong = readProp(props, header, name, Long.class);
        return (asLong != null) ? asLong.intValue() : null;
    }

    @Nullable
    private static Double readDoubleProp(@Nonnull final ByteBuffer props,
                                         @Nonnull final HeaderMeta header,
                                         @Nonnull final String name) {
        return readProp(props, header, name, Double.class);
    }

    /**
     * Reads one typed value from {@code p} and advances its position accordingly.
     * Returns the decoded value, or {@code null} if the type is unrecognised.
     */
    @Nullable
    private static Object readTypedValue(@Nonnull final ByteBuffer p, final byte colType) {
        switch (colType) {
            case ColumnType.Byte:
                return (p.remaining() >= 1) ? (int) p.get() : null;
            case ColumnType.UByte:
                return (p.remaining() >= 1) ? (p.get() & 0xFF) : null;
            case ColumnType.Bool:
                return (p.remaining() >= 1) ? (p.get() != 0) : null;
            case ColumnType.Short:
                return (p.remaining() >= 2) ? (int) p.getShort() : null;
            case ColumnType.UShort:
                return (p.remaining() >= 2) ? (p.getShort() & 0xFFFF) : null;
            case ColumnType.Int:
                return (p.remaining() >= 4) ? p.getInt() : null;
            case ColumnType.UInt:
                return (p.remaining() >= 4) ? (p.getInt() & 0xFFFFFFFFL) : null;
            case ColumnType.Long:
                return (p.remaining() >= 8) ? p.getLong() : null;
            case ColumnType.ULong:
                return (p.remaining() >= 8) ? p.getLong() : null;
            case ColumnType.Float:
                return (p.remaining() >= 4) ? (double) p.getFloat() : null;
            case ColumnType.Double:
                return (p.remaining() >= 8) ? p.getDouble() : null;
            case ColumnType.String:
            case ColumnType.Json:
            case ColumnType.DateTime: {
                if (p.remaining() < 4) {
                    return null;
                }
                final int len = p.getInt();
                if (len < 0 || len > p.remaining()) {
                    return null;
                }
                final byte[] strBytes = new byte[len];
                p.get(strBytes);
                return new String(strBytes, StandardCharsets.UTF_8);
            }
            case ColumnType.Binary: {
                // Consume the 4-byte length prefix and the payload bytes without decoding.
                if (p.remaining() < 4) {
                    return null;
                }
                final int len = p.getInt();
                if (len < 0 || len > p.remaining()) {
                    return null;
                }
                p.position(p.position() + len);
                return null;
            }
            default:
                return null;
        }
    }

    @Nonnull
    public List<TerritoryMatch> lookup(final double latDeg, final double lonDeg) {
        final Coordinate coord = new Coordinate(lonDeg, latDeg);
        final Envelope env = new Envelope(coord);
        @SuppressWarnings("unchecked")
        final List<IndexedEntry> candidates = index.query(env);
        final Point point = GEOMETRY_FACTORY.createPoint(coord);
        final List<TerritoryMatch> hits = new ArrayList<>(candidates.size());
        for (final IndexedEntry e : candidates) {
            if (e.prepared.contains(point)) {
                hits.add(new TerritoryMatch(e.alphaCode, e.parentAlphaCode, e.adminLevel, e.area));
            }
        }
        hits.sort(Comparator
                .comparingInt(TerritoryMatch::getAdminLevel).reversed() // level 4 before level 2
                .thenComparingDouble(TerritoryMatch::getArea));          // smaller area first
        return hits;
    }

    private static final class IndexedEntry {
        @Nonnull final PreparedGeometry prepared;
        @Nonnull final String alphaCode;
        @Nullable final String parentAlphaCode;
        final int adminLevel;
        final double area;

        IndexedEntry(
                @Nonnull final PreparedGeometry prepared,
                @Nonnull final String alphaCode,
                @Nullable final String parentAlphaCode,
                final int adminLevel,
                final double area) {
            this.prepared = prepared;
            this.alphaCode = alphaCode;
            this.parentAlphaCode = parentAlphaCode;
            this.adminLevel = adminLevel;
            this.area = area;
        }
    }
}
