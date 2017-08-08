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

import javax.annotation.Nullable;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.TimeoutHandler;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TestAsyncResponse implements AsyncResponse {

    private boolean isResumed = false;
    private Object response = null;

    @Override
    public boolean resume(final Object response) {
        this.isResumed = true;
        this.response = response;
        return true;
    }

    @Override
    public boolean resume(final Throwable response) {
        this.isResumed = true;
        this.response = response;
        return true;
    }

    @Override
    public boolean cancel() {
        this.isResumed = false;
        return true;
    }

    @Override
    public boolean cancel(final int retryAfter) {
        this.isResumed = false;
        return true;
    }

    @Override
    public boolean cancel(final Date retryAfter) {
        this.isResumed = false;
        return true;
    }

    @Override
    public boolean isSuspended() {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean setTimeout(final long time, final TimeUnit unit) {
        return true;
    }

    @Override
    public void setTimeoutHandler(final TimeoutHandler handler) {
        // Empty.
    }

    @Override
    @Nullable
    public Collection<Class<?>> register(final Class<?> callback) {
        return null;
    }

    @Override
    @Nullable
    public Map<Class<?>, Collection<Class<?>>> register(final Class<?> callback, final Class<?>... callbacks) {
        return null;
    }

    @Override
    @Nullable
    public Collection<Class<?>> register(final Object callback) {
        return null;
    }

    @Override
    @Nullable
    public Map<Class<?>, Collection<Class<?>>> register(final Object callback, final Object... callbacks) {
        return null;
    }

    public boolean isResumed() {
        return isResumed;
    }

    @Nullable
    public Object getResponse() {
        return response;
    }
}
