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

package com.mapcode.services.domain;

import com.tomtom.speedtools.apivalidation.ApiDataBinder;
import com.mapcode.services.ApiConstants;
import com.tomtom.speedtools.objects.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@SuppressWarnings({"NonFinalFieldReferenceInEquals", "NonFinalFieldReferencedInHashCode"})
public final class VersionBinder extends ApiDataBinder {

    @Nonnull
    private String version;

    @Override
    public void validate() {
        validator().start();
        validator().checkString(true, "version", version,
                ApiConstants.API_VERSION_LEN_MIN,
                ApiConstants.API_VERSION_LEN_MAX);
        validator().done();
    }

    public VersionBinder(@Nonnull final String version) {
        this.version = version;
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    private VersionBinder() {
        // Default constructor required by JAX-B.
        super();
    }

    @Nonnull
    public String getVersion() {
        beforeGet();
        return version;
    }

    public void setVersion(@Nonnull final String version) {
        beforeSet();
        assert version != null;
        this.version = version;
    }

    @Override
    public boolean canEqual(@Nonnull final Object obj) {
        assert obj != null;
        return obj instanceof VersionBinder;
    }

    @Override
    public boolean equals(@Nullable final Object obj) {
        boolean eq;
        if (this == obj) {
            eq = true;
        } else if ((obj != null) && (obj instanceof VersionBinder)) {
            final VersionBinder that = (VersionBinder) obj;
            eq = that.canEqual(this);
            eq = eq && (this.version.equals(that.version));
        } else {
            eq = false;
        }

        return eq;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(version);
    }
}
