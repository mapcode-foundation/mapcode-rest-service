/*
 * Copyright (C) 2016-2020, Stichting Mapcode Foundation (http://www.mapcode.com)
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

package com.mapcode.services.standalone;

import com.google.inject.Binder;
import com.google.inject.Module;

import javax.annotation.Nonnull;

/**
 * This module is used only in stand-alone (CLI) mode. It inserts a default properties
 * file into the server.
 */
public class StandaloneModule implements Module {

    @Override
    public void configure(@Nonnull final Binder binder) {
        assert binder != null;
        binder.bind(Server.class).asEagerSingleton();
    }
}
