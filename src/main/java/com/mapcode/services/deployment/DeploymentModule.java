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

package com.mapcode.services.deployment;

import com.google.inject.Binder;
import com.tomtom.speedtools.guice.GuiceConfigurationModule;

import javax.annotation.Nonnull;


/**
 * This class defines the deployment configuration for Google Guice.
 *
 * The deployment module "bootstraps" the whole Guice injection process.
 *
 * It bootstraps the Guice injection and specifies the property files to be read. It also needs to bind the tracer, so
 * they can be used early on in the app. Finally, it can bind a "startup check" (example provided) as an eager
 * singleton, so the system won't start unless a set of basic preconditions are fulfilled.
 *
 * The "speedtools.default.properties" is required, but its values may be overridden in other property files.
 */
public class DeploymentModule extends GuiceConfigurationModule {

    public DeploymentModule() {
        super(
            "classpath:speedtools.default.properties",      // Default set required by SpeedTools.
            "classpath:services.properties");               // Additional property file(s).
    }

    @Override
    public void configure(@Nonnull final Binder binder) {
        assert binder != null;

        super.configure(binder);

        // Bind start-up checking class (example).
        binder.bind(StartupCheck.class).asEagerSingleton();
    }
}
