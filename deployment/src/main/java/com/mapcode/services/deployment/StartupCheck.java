/*
 * Copyright (C) 2016-2019, Stichting Mapcode Foundation (http://www.mapcode.com)
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

package com.mapcode.services.deployment;

import com.google.inject.Injector;
import com.mapcode.services.jmx.SystemMetricsAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.nio.charset.Charset;

/**
 * This class provides a safe start up of the system. It provides basic checks required before startup is allowed. It
 * is
 * abound as an eager Singleton by Guice in the {@link DeploymentModule}
 * by using "binder.bind(StartupCheck.class).asEagerSingleton()". This means the Singleton is instantiated right away.
 * <p> If any of the checks in the constructor of this class fail, an IllegalStateException is thrown and the
 * application will not continue to run. This prevents starting the system with, for example, incorrect database
 * information or an incompatible JRE.
 */
public final class StartupCheck {
    private static final Logger LOG = LoggerFactory.getLogger(StartupCheck.class);

    private static final String REQUIRED_ENCODING = "UTF-8";

    @Inject
    StartupCheck(@Nonnull final Injector injector) {
        assert injector != null;

        /**
         * This method contains a number of checks that should be performed before the system
         * is started up. Not every application will need a 'StartupCheck' class, but if it does
         * it is good to know how to implement it safely in the SpeedTools framework. Hence the
         * inclusion in this example.
         *
         * A good example of a really useful StartupCheck is to check whether the database has
         * the expected format, especially for no-SQL databases. If the database format (or
         * schema, as you may wish to call it) is not what you think it is, you might ruin the
         * database. You should check this sort of stuff here.
         *
         * Another good one is checking the expected character set (see below) and the JRE version.
         */

        // Check if we are using the correct JDK.
        final String javaVersion = System.getProperty("java.version");
        check(javaVersion.startsWith("1.6.") || javaVersion.startsWith("1.7.") || javaVersion.startsWith("1.8."),
                "The system requires JRE 1.6.x, 1.7.x or 1.8.x (found JRE " + javaVersion + ").");

        // Check encoding. The default character encoding for JSON is UTF8. UTF16 and UTF32 are also supported.
        // This is to make sure that byte conversions that rely on default encoding do not cause unexpected behaviour.
        check(REQUIRED_ENCODING.equals(Charset.defaultCharset().name()),
                "The system default encoding must be UTF-8 (add '-Dfile.encoding=UTF-8' the JVM command line)." +
                        " Current value=" + Charset.defaultCharset().name());

        // Start JMX server.
        final SystemMetricsAgent jmxAgent = injector.getInstance(SystemMetricsAgent.class);
        try {
            jmxAgent.register();
        }
        catch (final Exception e) {
            check(false, "Could not register the JMX agent: " + e.getMessage());
        }

        LOG.info("Startup: System started successfully.");
    }

    private StartupCheck() {
        // Empty.
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private static void check(final boolean check, @Nonnull final String reason) {
        if (!check) {
            LOG.error("check: System did NOT start succesfully.");
            LOG.error("check: Reason: {}", reason);

            System.err.println();
            System.err.println("=======================================");
            System.err.println("ERROR");
            System.err.println("=======================================");
            System.err.println("System did NOT start successfully.");
            System.err.println("Reason: " + reason);
            System.err.println("=======================================");
            System.err.println();
            throw new IllegalStateException("System did NOT start successfully.");
        }
    }
}
