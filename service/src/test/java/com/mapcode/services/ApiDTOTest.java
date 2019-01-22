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

package com.mapcode.services;

import com.mapcode.Alphabet;
import com.mapcode.services.dto.*;
import com.tomtom.speedtools.objects.Immutables;
import org.junit.Assert;
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
        AlphabetDTO x = new AlphabetDTO("x");
        assertEquals("x", x.getName());

        x = new AlphabetDTO("x");
        x.setName("y");
        assertEquals("y", x.getName());
    }

    @Test
    public void checkAlphabetListDTO() {
        LOG.info("checkAlphabetListDTO");
        AlphabetListDTO x = new AlphabetListDTO(Immutables.listOf(new AlphabetDTO("x")));
        assertEquals(1, x.size());
        assertEquals("x", x.get(0).getName());
    }

    @Test
    public void checkAlphabetsDTO() {
        LOG.info("checkAlphabetsDTO");
        AlphabetsDTO x = new AlphabetsDTO(1, new AlphabetListDTO(Immutables.listOf(new AlphabetDTO("x"))));
        assertEquals(1, x.getTotal());
        assertEquals(1, x.getAlphabets().size());

        x = new AlphabetsDTO(1, new AlphabetListDTO(Immutables.listOf(new AlphabetDTO("x"))));
        x.setAlphabets(new AlphabetListDTO(Immutables.listOf(new AlphabetDTO("y"))));
        x.setTotal(12);
        assertEquals(12, x.getTotal());
        assertEquals(1, x.getAlphabets().size());
        assertEquals("y", x.getAlphabets().get(0).getName());

        final Alphabet[] a = {Alphabet.ROMAN, Alphabet.GREEK, Alphabet.ARABIC};
        x = new AlphabetsDTO(a.length, new AlphabetListDTO(a));
        assertEquals(3, x.getTotal());
        assertEquals(3, x.getAlphabets().size());
        assertEquals("ROMAN", x.getAlphabets().get(0).getName());
        assertEquals("GREEK", x.getAlphabets().get(1).getName());
        assertEquals("ARABIC", x.getAlphabets().get(2).getName());
    }

    @Test
    public void checkCoordinatesDTO() {
        LOG.info("checkCoordinatesDTO");
        PointDTO x = new PointDTO(1.0, 2.0);
        assertEquals(1.0, x.getLatDeg(), 0.01);
        assertEquals(2.0, x.getLonDeg(), 0.01);

        x = new PointDTO(1.0, 2.0);
        x.setLatDeg(-90.0);
        x.setLonDeg(-180.0);
        assertEquals(-90.0, x.getLatDeg(), 0.01);
        assertEquals(-180.0, x.getLonDeg(), 0.01);
    }

    @Test
    public void checkMapcodeDTO() {
        LOG.info("checkMapcodeDTO");
        MapcodeDTO x = new MapcodeDTO("XX.XX", "YY.YY", "NLD", "BEL", 1.0);
        assertEquals("XX.XX", x.getMapcode());
        assertEquals("YY.YY", x.getMapcodeInAlphabet());
        assertEquals("NLD", x.getTerritory());
        assertEquals("BEL", x.getTerritoryInAlphabet());
        assertEquals(1.0, x.getOffsetMeters(), 0.01);

        x = new MapcodeDTO("XX.XX", "YY.YY", "NLD", "BEL", 1.0);
        x.setMapcode("11.11");
        x.setMapcodeInAlphabet("\u0397\u03a0.\u03982-\u0411");
        x.setTerritory("\u0393\u03a8\u039e");
        x.setTerritoryInAlphabet("444");
        assertEquals("11.11", x.getMapcode());
        assertEquals("\u0397\u03a0.\u03982-\u0411", x.getMapcodeInAlphabet());
        assertEquals("\u0393\u03a8\u039e", x.getTerritory());
        assertEquals("444", x.getTerritoryInAlphabet());
        assertEquals(1.0, x.getOffsetMeters(), 0.01);
    }

    @Test
    public void checkMapcodeListDTO() {
        LOG.info("checkMapcodeListDTO");
        MapcodeListDTO x = new MapcodeListDTO(Immutables.listOf(new MapcodeDTO("XX.XX", "YY.YY", "NLD", "BEL", 1.0)));
        assertEquals(1, x.size());
        assertEquals("XX.XX", x.get(0).getMapcode());
    }

    @Test
    public void checkMapcodesDTO() {
        LOG.info("checkMapcodesDTO");
        MapcodesDTO x = new MapcodesDTO(
                new MapcodeDTO("AA.AA", "aa.aa", "USA", "usa", 1.0),
                new MapcodeDTO("BB.BB", "bb.bb", "CAN", "can", 1.0),
                new MapcodeListDTO(Immutables.listOf(new MapcodeDTO("XX.XX", "YY.YY", "NLD", "BEL", 1.0))));
        Assert.assertNotNull(x.getLocal());
        assertEquals("AA.AA", x.getLocal().getMapcode());
        assertEquals("bb.bb", x.getInternational().getMapcodeInAlphabet());
        assertEquals("BEL", x.getMapcodes().get(0).getTerritoryInAlphabet());

        x = new MapcodesDTO(
                new MapcodeDTO("AA.AA", "aa.aa", "USA", "usa", 1.0),
                new MapcodeDTO("BB.BB", "bb.bb", "CAN", "can", 1.0),
                new MapcodeListDTO(Immutables.listOf(new MapcodeDTO("XX.XX", "YY.YY", "NLD", "BEL", 1.0))));
        x.setLocal(new MapcodeDTO("11.11", "22.22", "333", "444", 1.0));
        x.setInternational(new MapcodeDTO("10.10", "20.20", "300", "400", 1.0));
        x.setMapcodes(new MapcodeListDTO(Immutables.listOf(new MapcodeDTO("12.34", "43.21", "USA", "CAN", 1.0))));
        Assert.assertNotNull(x.getLocal());
        assertEquals("11.11", x.getLocal().getMapcode());
        assertEquals("20.20", x.getInternational().getMapcodeInAlphabet());
        assertEquals("CAN", x.getMapcodes().get(0).getTerritoryInAlphabet());
    }

    @Test
    public void checkTerritoryDTO() {
        LOG.info("checkTerritoryDTO");
        final AlphabetListDTO a = new AlphabetListDTO(new Alphabet[]{Alphabet.ROMAN, Alphabet.GREEK, Alphabet.ARABIC});
        TerritoryDTO x = new TerritoryDTO("a", "b", "c", "d", "e", new String[]{"f", "g"}, new String[]{"h", "i"}, a);
        assertEquals("a", x.getAlphaCode());
        assertEquals("b", x.getAlphaCodeMinimalUnambiguous());
        assertEquals("c", x.getAlphaCodeMinimal());
        assertEquals("d", x.getFullName());
        assertEquals("e", x.getParentTerritory());
        assertEquals(2, x.getAliases().length);
        assertEquals("f", x.getAliases()[0]);
        assertEquals("g", x.getAliases()[1]);
        assertEquals(2, x.getFullNameAliases().length);
        assertEquals("h", x.getFullNameAliases()[0]);
        assertEquals("i", x.getFullNameAliases()[1]);

        x = new TerritoryDTO("a", "b", "c", "d", "e", new String[]{"f", "g"}, new String[]{"h", "i"}, a);
        x.setAlphaCode("1");
        x.setAlphaCodeMinimalUnambiguous("2");
        x.setAlphaCodeMinimal("3");
        x.setFullName("4");
        x.setParentTerritory("5");
        x.setAliases(new String[]{"6"});
        x.setFullNameAliases(new String[]{"7"});
        assertEquals("1", x.getAlphaCode());
    }

    @Test
    public void checkTerritoryListDTO() {
        LOG.info("checkTerritoryListDTO");
        final AlphabetListDTO a = new AlphabetListDTO(new Alphabet[]{Alphabet.ROMAN, Alphabet.GREEK, Alphabet.ARABIC});
        TerritoryListDTO x = new TerritoryListDTO(Immutables.listOf(
                new TerritoryDTO("a", "b", "c", "d", "e", new String[]{"f", "g"}, new String[]{"h", "i"}, a)));
        assertEquals(1, x.size());
        assertEquals("a", x.get(0).getAlphaCode());
    }

    @Test
    public void checkTerritoriesDTO() {
        LOG.info("checkTerritoriesDTO");
        final AlphabetListDTO a = new AlphabetListDTO(new Alphabet[]{Alphabet.ROMAN, Alphabet.GREEK, Alphabet.ARABIC});
        TerritoriesDTO x = new TerritoriesDTO(10,
                new TerritoryListDTO(Immutables.listOf(
                        new TerritoryDTO("a", "b", "c", "d", "e", new String[]{"f", "g"}, new String[]{"h", "i"}, a))));
        assertEquals(10, x.getTotal());
        assertEquals("a", x.getTerritories().get(0).getAlphaCode());

        x = new TerritoriesDTO(100,
                new TerritoryListDTO(Immutables.listOf(
                        new TerritoryDTO("a", "b", "c", "d", "e", new String[]{"f", "g"}, new String[]{"h", "i"}, a))));
        x.setTotal(12);
        x.setTerritories(new TerritoryListDTO(Immutables.listOf(
                new TerritoryDTO("1", "2", "3", "4", "5", new String[]{"6"}, new String[]{"7"}, a))));
        assertEquals(12, x.getTotal());
        assertEquals("1", x.getTerritories().get(0).getAlphaCode());
    }

    @Test
    public void checkVersionDTO() {
        LOG.info("checkVersionDTO");
        VersionDTO x = new VersionDTO("x");
        assertEquals("x", x.getVersion());

        x = new VersionDTO("x");
        x.setVersion("2");
        assertEquals("2", x.getVersion());
    }
}
