/*
 * Copyright (C) 2016, Stichting Mapcode Foundation (http://www.mapcode.com)
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

package com.mapcode.services.jmx;

import com.mapcode.services.SystemMetrics;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 * JMX agent that serves {@link SystemMetrics} through JMX.
 */
public class SystemMetricsAgent {

    @Nonnull
    private final SystemMetrics systemMetrics;

    @Inject
    public SystemMetricsAgent(@Nonnull final SystemMetrics systemMetrics) {
        assert systemMetrics != null;
        this.systemMetrics = systemMetrics;
    }

    public void register() throws MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException,
            MBeanRegistrationException {

        // Get the platform MBeanServer.
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        // Uniquely identify the MBeans and register them with the platform MBeanServer.
        final ObjectName name = new ObjectName("mapcode:name=SystemMetrics");

        // Unregister old bean. Needed for tests.
        try {
            mbs.unregisterMBean(name);
        } catch (final InstanceNotFoundException ignored) {
            // Ignored.
        }

        // Register metrics bean.
        mbs.registerMBean(systemMetrics, name);
    }
}
