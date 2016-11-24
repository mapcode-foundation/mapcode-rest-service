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

package com.mapcode.services.deployment;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.name.Names;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@SuppressWarnings("rawtypes")
public class DeploymentModuleTest {
    private static final Logger LOG = LoggerFactory.getLogger(DeploymentModuleTest.class);

    @Mock
    private Binder mockBinder;

    @Mock
    private AnnotatedBindingBuilder mockAnnotatedBindingBuilder;

    @Mock
    private LinkedBindingBuilder mockLinkedBindingBuilder;

    @SuppressWarnings("unchecked")
    @Test
    public void testDeploymentModule() {
        LOG.info("testDeploymentModule");

        // Initialize Mockito.
        MockitoAnnotations.initMocks(this);
        doNothing().when(mockAnnotatedBindingBuilder).asEagerSingleton();
        doNothing().when(mockLinkedBindingBuilder).asEagerSingleton();
        doNothing().when(mockLinkedBindingBuilder).toInstance(String.class);
        when(mockBinder.skipSources(Names.class)).thenReturn(mockBinder);
        when(mockBinder.bind(any(Key.class))).thenReturn(mockLinkedBindingBuilder);
        when(mockBinder.bind(any(Class.class))).thenReturn(mockAnnotatedBindingBuilder);

        // Execute start-up check.
        final DeploymentModule deploymentModule = new DeploymentModule();
        deploymentModule.configure(mockBinder);
        Assert.assertNotNull(deploymentModule);
    }
}
