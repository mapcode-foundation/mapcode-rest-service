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

package com.mapcode.services.deployment;

import com.google.inject.Injector;
import com.mapcode.services.jmx.SystemMetricsAgent;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

public class StartupCheckTest {
    private static final Logger LOG = LoggerFactory.getLogger(StartupCheckTest.class);

    @Mock
    private Injector mockInjector;

    @Mock
    private SystemMetricsAgent mockJmxAgent;

    @SuppressWarnings("InstantiationOfUtilityClass")
    @Test(expected = IllegalStateException.class)
    public void testStartupCheckFails() {
        LOG.info("testStartupCheckFailure");

        // Initialize Mockito.
        MockitoAnnotations.initMocks(this);
        when(mockInjector.getInstance(SystemMetricsAgent.class)).thenReturn(null);

        // Execute start-up check.
        //noinspection ResultOfObjectAllocationIgnored
        new StartupCheck(mockInjector);
    }

    @SuppressWarnings("InstantiationOfUtilityClass")
    @Test
    public void testStartupCheckSuccess() throws Exception {
        LOG.info("testStartupCheckSuccess");

        // Initialize Mockito.
        MockitoAnnotations.initMocks(this);
        doNothing().when(mockJmxAgent).register();
        when(mockInjector.getInstance(SystemMetricsAgent.class)).thenReturn(mockJmxAgent);

        // Execute start-up check.
        final StartupCheck startupCheck = new StartupCheck(mockInjector);
        Assert.assertNotNull(startupCheck);
    }
}
