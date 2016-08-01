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

import com.mapcode.services.dto.*;
import com.tomtom.speedtools.objects.Immutables;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

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

        AlphabetsDTO x2 = new AlphabetsDTO(1, new AlphabetListDTO(Immutables.listOf(new AlphabetDTO("x"))));
        x2.setAlphabets(new AlphabetListDTO(Immutables.listOf(new AlphabetDTO("y"))));
        x2.setTotal(12);
        assertEquals(12, x2.getTotal());
        assertEquals(1, x2.getAlphabets().size());
        assertEquals("y", x2.getAlphabets().get(0).getName());
    }

    @Test
    public void checkCoordinatesDTO() {
        LOG.info("checkCoordinatesDTO");
        CoordinatesDTO x1 = new CoordinatesDTO(1.0, 2.0);
        assertEquals(1.0, x1.getLatDeg(), 0.01);
        assertEquals(2.0, x1.getLonDeg(), 0.01);

        CoordinatesDTO x2 = new CoordinatesDTO(1.0, 2.0);
        x2.setLatDeg(-90.0);
        x2.setLonDeg(-180.0);
        assertEquals(-90.0, x2.getLatDeg(), 0.01);
        assertEquals(-180.0, x2.getLonDeg(), 0.01);
    }

    @Test
    public void checkMapcodeDTO() {
        LOG.info("checkMapcodeDTO");
        MapcodeDTO x1 = new MapcodeDTO("XX.XX", "YY.YY", "NLD", "BEL", 1.0);
        assertEquals("XX.XX", x1.getMapcode());
        assertEquals("YY.YY", x1.getMapcodeInAlphabet());
        assertEquals("NLD", x1.getTerritory());
        assertEquals("BEL", x1.getTerritoryInAlphabet());
        assertEquals(1.0, x1.getOffsetMeters(), 0.01);

        MapcodeDTO x2 = new MapcodeDTO("XX.XX", "YY.YY", "NLD", "BEL", 1.0);
        x2.setMapcode("11.11");
        x2.setMapcodeInAlphabet("\u0397\u03a0.\u03982-\u0411");
        x2.setTerritory("\u0393\u03a8\u039e");
        x2.setTerritoryInAlphabet("444");
        assertEquals("11.11", x2.getMapcode());
        assertEquals("\u0397\u03a0.\u03982-\u0411", x2.getMapcodeInAlphabet());
        assertEquals("\u0393\u03a8\u039e", x2.getTerritory());
        assertEquals("444", x2.getTerritoryInAlphabet());
        assertEquals(1.0, x1.getOffsetMeters(), 0.01);
    }

    @Test
    public void checkMapcodeListDTO() {
        LOG.info("checkMapcodeListDTO");
        MapcodeListDTO x1 = new MapcodeListDTO(Immutables.listOf(new MapcodeDTO("XX.XX", "YY.YY", "NLD", "BEL", 1.0)));
        assertEquals(1, x1.size());
        assertEquals("XX.XX", x1.get(0).getMapcode());
    }

    @Test
    public void checkMapcodesDTO() {
        LOG.info("checkMapcodesDTO");
        MapcodesDTO x1 = new MapcodesDTO(
                new MapcodeDTO("AA.AA", "aa.aa", "USA", "usa", 1.0),
                new MapcodeDTO("BB.BB", "bb.bb", "CAN", "can", 1.0),
                new MapcodeListDTO(Immutables.listOf(new MapcodeDTO("XX.XX", "YY.YY", "NLD", "BEL", 1.0))));
        assertEquals("AA.AA", x1.getLocal().getMapcode());
        assertEquals("bb.bb", x1.getInternational().getMapcodeInAlphabet());
        assertEquals("BEL", x1.getMapcodes().get(0).getTerritoryInAlphabet());

        MapcodesDTO x2 = new MapcodesDTO(
                new MapcodeDTO("AA.AA", "aa.aa", "USA", "usa", 1.0),
                new MapcodeDTO("BB.BB", "bb.bb", "CAN", "can", 1.0),
                new MapcodeListDTO(Immutables.listOf(new MapcodeDTO("XX.XX", "YY.YY", "NLD", "BEL", 1.0))));
        x2.setLocal(new MapcodeDTO("11.11", "22.22", "333", "444", 1.0));
        x2.setInternational(new MapcodeDTO("10.10", "20.20", "300", "400", 1.0));
        x2.setMapcodes(new MapcodeListDTO(Immutables.listOf(new MapcodeDTO("12.34", "43.21", "USA", "CAN", 1.0))));
        assertEquals("11.11", x2.getLocal().getMapcode());
        assertEquals("20.20", x2.getInternational().getMapcodeInAlphabet());
        assertEquals("CAN", x2.getMapcodes().get(0).getTerritoryInAlphabet());
    }

    @Test
    public void checkTerritoryDTO() {
        LOG.info("checkTerritoryDTO");
        TerritoryDTO x1 = new TerritoryDTO("a", "b", "c", "d", "e", new String[]{"f", "g"}, new String[]{"h", "i"});
        assertEquals("a", x1.getAlphaCode());
        assertEquals("b", x1.getAlphaCodeMinimalUnambiguous());
        assertEquals("c", x1.getAlphaCodeMinimal());
        assertEquals("d", x1.getFullName());
        assertEquals("e", x1.getParentTerritory());
        assertEquals(2, x1.getAliases().length);
        assertEquals("f", x1.getAliases()[0]);
        assertEquals("g", x1.getAliases()[1]);
        assertEquals(2, x1.getFullNameAliases().length);
        assertEquals("h", x1.getFullNameAliases()[0]);
        assertEquals("i", x1.getFullNameAliases()[1]);

        TerritoryDTO x2 = new TerritoryDTO("a", "b", "c", "d", "e", new String[]{"f", "g"}, new String[]{"h", "i"});
        x2.setAlphaCode("1");
        x2.setAlphaCodeMinimalUnambiguous("2");
        x2.setAlphaCodeMinimal("3");
        x2.setFullName("4");
        x2.setParentTerritory("5");
        x2.setAliases(new String[]{"6"});
        x2.setFullNameAliases(new String[]{"7"});
        assertEquals("1", x2.getAlphaCode());
    }

    @Test
    public void checkTerritoryListDTO() {
        LOG.info("checkTerritoryListDTO");
        TerritoryListDTO x1 = new TerritoryListDTO(Immutables.listOf(
                new TerritoryDTO("a", "b", "c", "d", "e", new String[]{"f", "g"}, new String[]{"h", "i"})));
        assertEquals(1, x1.size());
        assertEquals("a", x1.get(0).getAlphaCode());
    }

    @Test
    public void checkTerritoriesDTO() {
        LOG.info("checkTerritoriesDTO");
        TerritoriesDTO x1 = new TerritoriesDTO(10,
                new TerritoryListDTO(Immutables.listOf(
                        new TerritoryDTO("a", "b", "c", "d", "e", new String[]{"f", "g"}, new String[]{"h", "i"}))));
        assertEquals(10, x1.getTotal());
        assertEquals("a", x1.getTerritories().get(0).getAlphaCode());

        TerritoriesDTO x2 = new TerritoriesDTO(100,
                new TerritoryListDTO(Immutables.listOf(
                        new TerritoryDTO("a", "b", "c", "d", "e", new String[]{"f", "g"}, new String[]{"h", "i"}))));
        x2.setTotal(12);
        x2.setTerritories(new TerritoryListDTO(Immutables.listOf(
                new TerritoryDTO("1", "2", "3", "4", "5", new String[]{"6"}, new String[]{"7"}))));
        assertEquals(12, x2.getTotal());
        assertEquals("1", x2.getTerritories().get(0).getAlphaCode());
    }

    @Test
    public void checkVersionDTO() {
        LOG.info("checkVersionDTO");
        VersionDTO x1 = new VersionDTO("x");
        assertEquals("x", x1.getVersion());

        VersionDTO x2 = new VersionDTO("x");
        x2.setVersion("2");
        assertEquals("2", x2.getVersion());
    }
}
