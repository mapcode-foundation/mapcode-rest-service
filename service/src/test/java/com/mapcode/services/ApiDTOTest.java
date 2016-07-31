/*
 * Copyright (C) 2016 Stichting Mapcode Foundation (http://www.mapcode.com)
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

package com.mapcode.services;

import com.mapcode.services.dto.AlphabetDTO;
import com.mapcode.services.dto.AlphabetListDTO;
import com.mapcode.services.dto.AlphabetsDTO;
import com.mapcode.services.dto.CoordinatesDTO;
import com.mongodb.annotations.Immutable;
import com.tomtom.speedtools.objects.Immutables;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("JUnitTestMethodWithNoAssertions")
public class ApiDTOTest {
    private static final Logger LOG = LoggerFactory.getLogger(ApiDTOTest.class);

    @Test
    public void checkAlphabetDTO() {
        LOG.info("checkAlphabetDTO");
        AlphabetDTO x1 = new AlphabetDTO("x");
        assertEquals("x", x1.getName());

        AlphabetDTO x2 = new AlphabetDTO("x");
        x2.setName("y");
        assertEquals("y", x2.getName());
    }

    @Test
    public void checkAlphabetListDTO() {
        LOG.info("checkAlphabetListDTO");
        AlphabetListDTO x1 = new AlphabetListDTO(Immutables.listOf(new AlphabetDTO("x")));
        assertEquals(1, x1.size());
        assertEquals("x", x1.get(0).getName());
    }

    @Test
    public void checkAlphabetsDTO() {
        LOG.info("checkAlphabetsDTO");
        AlphabetsDTO x1 = new AlphabetsDTO(1, new AlphabetListDTO(Immutables.listOf(new AlphabetDTO("x"))));
        assertEquals(1, x1.getTotal());
        assertEquals(1, x1.getAlphabets().size());
    }

    @Test
    public void checkCoordinatesDTO() {
        LOG.info("checkCoordinatesDTO");
        CoordinatesDTO x1 = new CoordinatesDTO(1.0, 2.0);
        assertEquals(1, x1.getLatDeg().intValue());
        assertEquals(2, x1.getLonDeg().intValue());
    }
}
