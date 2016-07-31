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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mapcode.Alphabet;
import com.mapcode.services.dto.AlphabetsDTO;
import com.mapcode.services.dto.VersionDTO;
import com.tomtom.speedtools.json.Json;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@SuppressWarnings("JUnitTestMethodWithNoAssertions")
public class ApiAlphabetsTest {
    private static final Logger LOG = LoggerFactory.getLogger(ApiAlphabetsTest.class);

    private static final Double TEST_LAT1 = 50.141706;
    private static final Double TEST_LON1 = 6.135864;
    private static final String TEST_LATLON1 = TEST_LAT1 + "," + TEST_LON1;

    private LocalTestServer server;

    @Before
    public void startServer() {
        server = new LocalTestServer("1.0", 8081);
        server.start();
    }

    @After
    public void stopServer() {
        server.stop();
    }

    @Test
    public void checkAlphabetsJson() {
        LOG.info("checkAlphabetsJson");
        final Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/alphabets")).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        final String s = response.readEntity(String.class);
        Assert.assertEquals("{\"total\":14,\"alphabets\":[{\"name\":\"ROMAN\"},{\"name\":\"GREEK\"},{\"name\":\"CYRILLIC\"},{\"name\":\"HEBREW\"},{\"name\":\"HINDI\"},{\"name\":\"MALAY\"},{\"name\":\"GEORGIAN\"},{\"name\":\"KATAKANA\"},{\"name\":\"THAI\"},{\"name\":\"LAO\"},{\"name\":\"ARMENIAN\"},{\"name\":\"BENGALI\"},{\"name\":\"GURMUKHI\"},{\"name\":\"TIBETAN\"}]}",
                s);

        final AlphabetsDTO x = new Gson().fromJson(s, AlphabetsDTO.class);
        Assert.assertNotNull(x);
        Assert.assertEquals(14, x.getTotal());
        Assert.assertEquals("ROMAN", x.getAlphabets().get(0).getName());
    }

    @Test
    public void checkAlphabetsCountJson() {
        LOG.info("checkAlphabetsCountJson");
        final Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/alphabets?count=2")).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"total\":14,\"alphabets\":[{\"name\":\"ROMAN\"},{\"name\":\"GREEK\"}]}",
                response.readEntity(String.class));
    }

    @Test
    public void checkAlphabetsCountXml() {
        LOG.info("checkAlphabetsCountXml");
        final Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/alphabets?count=2")).
                request().
                accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><alphabets><total>14</total><alphabet><name>ROMAN</name></alphabet><alphabet><name>GREEK</name></alphabet></alphabets>",
                response.readEntity(String.class));
    }

    @Test
    public void checkAlphabetsCountOffsetJson() {
        LOG.info("checkAlphabetsCountOffsetJson");
        final Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/alphabets?count=1&offset=1")).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"total\":14,\"alphabets\":[{\"name\":\"GREEK\"}]}",
                response.readEntity(String.class));
    }

    @Test
    public void checkAlphabetsCountOffsetXml() {
        LOG.info("checkAlphabetsCountOffsetXml");
        final Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/alphabets?count=1&offset=1")).
                request().
                accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><alphabets><total>14</total><alphabet><name>GREEK</name></alphabet></alphabets>",
                response.readEntity(String.class));
    }

    @Test
    public void checkAlphabetsCountOffsetFromEndJson() {
        LOG.info("checkAlphabetsCountOffsetFromEndJson");
        final Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/alphabets?count=1&offset=-1")).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"total\":14,\"alphabets\":[{\"name\":\"TIBETAN\"}]}",
                response.readEntity(String.class));
    }

    @Test
    public void checkAlphabetsCountOffsetFromEndXml() {
        LOG.info("checkAlphabetsCountOffsetXml");
        final Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/alphabets?count=1&offset=-1")).
                request().
                accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><alphabets><total>14</total><alphabet><name>TIBETAN</name></alphabet></alphabets>",
                response.readEntity(String.class));
    }

    @Test
    public void checkAlphabetJson() {
        LOG.info("checkAlphabetJson");
        final Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/alphabets/greek")).
                request().
                accept(MediaType.APPLICATION_JSON_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("{\"name\":\"GREEK\"}",
                response.readEntity(String.class));
    }

    @Test
    public void checkAlphabetXml() {
        LOG.info("checkAlphabetXml");
        final Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/alphabets/greek")).
                request().
                accept(MediaType.APPLICATION_XML_TYPE).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><alphabet><name>GREEK</name></alphabet>",
                response.readEntity(String.class));
    }

    @Test
    public void checkAllAlphabetsJson() {
        LOG.info("checkAlphabetsJson");
        int i = 0;
        for (final Alphabet alphabet : Alphabet.values()) {
            doCheckAlphabet(alphabet.name(), MediaType.APPLICATION_JSON_TYPE, EXPECTED_ALPHABETS_JSON[i]);
            ++i;
        }
    }

    @Test
    public void checkAllAlphabetsXml() {
        LOG.info("checkAlphabetsXml");
        int i = 0;
        for (final Alphabet alphabet : Alphabet.values()) {
            doCheckAlphabet(alphabet.name(), MediaType.APPLICATION_XML_TYPE, EXPECTED_ALPHABETS_XML[i]);
            ++i;
        }
    }

    private void doCheckAlphabet(
            @Nonnull final String alphabet,
            @Nonnull final MediaType mediaType,
            @Nonnull final String expected) {
        LOG.info("doCheckAlphabet: alphabet={}, mediaType={}", alphabet, mediaType);
        final Response response = new ResteasyClientBuilder().build().
                target(server.url("/mapcode/codes/" + TEST_LATLON1 + "?include=territory,alphabet&alphabet=" + alphabet)).
                request().
                accept(mediaType).get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(expected, response.readEntity(String.class));
    }

    private final static String[] EXPECTED_ALPHABETS_JSON = {
            "{\"international\":{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"VJ0L6.9PNQ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"AAA\"},\"mapcodes\":[{\"mapcode\":\"JL0.KP\",\"mapcodeInAlphabet\":\"JL0.KP\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"LUX\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"R8RN.07Z\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"LUX\"},{\"mapcode\":\"SQB.NR3\",\"mapcodeInAlphabet\":\"SQB.NR3\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"BEL\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"R8RN.07Z\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"BEL\"},{\"mapcode\":\"0L46.LG9\",\"mapcodeInAlphabet\":\"0L46.LG9\",\"territory\":\"DEU\",\"territoryInAlphabet\":\"DEU\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"R8RN.07Z\",\"territory\":\"FRA\",\"territoryInAlphabet\":\"FRA\"},{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"VJ0L6.9PNQ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"AAA\"}]}",
            "{\"international\":{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"ΦΠ0Λ6.9ΡΝΘ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"ΑΑΑ\"},\"mapcodes\":[{\"mapcode\":\"JL0.KP\",\"mapcodeInAlphabet\":\"ΠΛ0.ΚΡ\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"LUX\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"Ψ8ΨΝ.07Ζ\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"LUX\"},{\"mapcode\":\"SQB.NR3\",\"mapcodeInAlphabet\":\"ΣΘΒ.ΝΨ3\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"BEL\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"Ψ8ΨΝ.07Ζ\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"BEL\"},{\"mapcode\":\"0L46.LG9\",\"mapcodeInAlphabet\":\"0Λ46.ΛΓ9\",\"territory\":\"DEU\",\"territoryInAlphabet\":\"DEU\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"Ψ8ΨΝ.07Ζ\",\"territory\":\"FRA\",\"territoryInAlphabet\":\"ΕΨΑ\"},{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"ΦΠ0Λ6.9ΡΝΘ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"ΑΑΑ\"}]}",
            "{\"international\":{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"ЧП0Л6.9РЗФ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"ААА\"},\"mapcodes\":[{\"mapcode\":\"JL0.KP\",\"mapcodeInAlphabet\":\"ПЛ0.КР\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"ЛЭХ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"Я8ЯЗ.07Б\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"ЛЭХ\"},{\"mapcode\":\"SQB.NR3\",\"mapcodeInAlphabet\":\"ЦФВ.ЗЯ3\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"ВЕЛ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"Я8ЯЗ.07Б\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"ВЕЛ\"},{\"mapcode\":\"0L46.LG9\",\"mapcodeInAlphabet\":\"0Л46.ЛГ9\",\"territory\":\"DEU\",\"territoryInAlphabet\":\"ДЕЭ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"Я8ЯЗ.07Б\",\"territory\":\"FRA\",\"territoryInAlphabet\":\"ЖЯА\"},{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"ЧП0Л6.9РЗФ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"ААА\"}]}",
            "{\"international\":{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"צט0ך6.9םלמ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"אאא\"},\"mapcodes\":[{\"mapcode\":\"JL0.KP\",\"mapcodeInAlphabet\":\"טך0.ים\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"ךץר\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"נ8נל.07ת\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"ךץר\"},{\"mapcode\":\"SQB.NR3\",\"mapcodeInAlphabet\":\"עמב.לנ3\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"בףך\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"נ8נל.07ת\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"בףך\"},{\"mapcode\":\"0L46.LG9\",\"mapcodeInAlphabet\":\"0ך46.ךז9\",\"territory\":\"DEU\",\"territoryInAlphabet\":\"דףץ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"נ8נל.07ת\",\"territory\":\"FRA\",\"territoryInAlphabet\":\"הנא\"},{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"צט0ך6.9םלמ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"אאא\"}]}",
            "{\"international\":{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"लठ0त6.9नधप\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"अअअ\"},\"mapcodes\":[{\"mapcode\":\"JL0.KP\",\"mapcodeInAlphabet\":\"ठत0.णन\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"तफस\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"भ8भध.07ड\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"तफस\"},{\"mapcode\":\"SQB.NR3\",\"mapcodeInAlphabet\":\"मपक.धभ3\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"कएत\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"भ8भध.07ड\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"कएत\"},{\"mapcode\":\"0L46.LG9\",\"mapcodeInAlphabet\":\"0त46.तज9\",\"territory\":\"DEU\",\"territoryInAlphabet\":\"घएफ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"भ8भध.07ड\",\"territory\":\"FRA\",\"territoryInAlphabet\":\"चभअ\"},{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"लठ0त6.9नधप\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"अअअ\"}]}",
            "{\"international\":{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"ഴഡ0ഥ6.9നധമ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"ഒഒഒ\"},\"mapcodes\":[{\"mapcode\":\"JL0.KP\",\"mapcodeInAlphabet\":\"ഡഥ0.തന\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"ഥഉശ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"ര8രധ.07ഹ\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"ഥഉശ\"},{\"mapcode\":\"SQB.NR3\",\"mapcodeInAlphabet\":\"റമക.ധര3\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"കഋഥ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"ര8രധ.07ഹ\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"കഋഥ\"},{\"mapcode\":\"0L46.LG9\",\"mapcodeInAlphabet\":\"0ഥ46.ഥജ9\",\"territory\":\"DEU\",\"territoryInAlphabet\":\"ഗഋഉ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"ര8രധ.07ഹ\",\"territory\":\"FRA\",\"territoryInAlphabet\":\"ചരഒ\"},{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"ഴഡ0ഥ6.9നധമ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"ഒഒഒ\"}]}",
            "{\"international\":{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"ႺႮ0Ⴑ6.9ႵႴႶ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"ႠႠႠ\"},\"mapcodes\":[{\"mapcode\":\"JL0.KP\",\"mapcodeInAlphabet\":\"ႮႱ0.ႰႵ\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"ႱႨႽ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"Ⴗ8ႷႴ.07Ⴟ\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"ႱႨႽ\"},{\"mapcode\":\"SQB.NR3\",\"mapcodeInAlphabet\":\"ႸႶႡ.ႴႷ3\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"ႡႤႱ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"Ⴗ8ႷႴ.07Ⴟ\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"ႡႤႱ\"},{\"mapcode\":\"0L46.LG9\",\"mapcodeInAlphabet\":\"0Ⴑ46.ႱႫ9\",\"territory\":\"DEU\",\"territoryInAlphabet\":\"ႦႤႨ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"Ⴗ8ႷႴ.07Ⴟ\",\"territory\":\"FRA\",\"territoryInAlphabet\":\"ႩႷႠ\"},{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"ႺႮ0Ⴑ6.9ႵႴႶ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"ႠႠႠ\"}]}",
            "{\"international\":{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"モス0ト6.9ヒヌフ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"アアア\"},\"mapcodes\":[{\"mapcode\":\"JL0.KP\",\"mapcodeInAlphabet\":\"スト0.チヒ\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"トエラ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"ヘ8ヘヌ.07ヲ\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"トエラ\"},{\"mapcode\":\"SQB.NR3\",\"mapcodeInAlphabet\":\"ホフカ.ヌヘ3\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"カオト\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"ヘ8ヘヌ.07ヲ\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"カオト\"},{\"mapcode\":\"0L46.LG9\",\"mapcodeInAlphabet\":\"0ト46.トコ9\",\"territory\":\"DEU\",\"territoryInAlphabet\":\"クオエ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"ヘ8ヘヌ.07ヲ\",\"territory\":\"FRA\",\"territoryInAlphabet\":\"ケヘア\"},{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"モス0ト6.9ヒヌフ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"アアア\"}]}",
            "{\"international\":{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"ลช0ด6.9ธทบ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"ะะะ\"},\"mapcodes\":[{\"mapcode\":\"JL0.KP\",\"mapcodeInAlphabet\":\"ชด0.ฑธ\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"ดฬอ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"ผ8ผท.07ฯ\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"ดฬอ\"},{\"mapcode\":\"SQB.NR3\",\"mapcodeInAlphabet\":\"มบก.ทผ3\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"กาด\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"ผ8ผท.07ฯ\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"กาด\"},{\"mapcode\":\"0L46.LG9\",\"mapcodeInAlphabet\":\"0ด46.ดจ9\",\"territory\":\"DEU\",\"territoryInAlphabet\":\"คาฬ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"ผ8ผท.07ฯ\",\"territory\":\"FRA\",\"territoryInAlphabet\":\"งผะ\"},{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"ลช0ด6.9ธทบ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"ะะะ\"}]}",
            "{\"international\":{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"ວຍ0ທ6.9ຜບພ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"ະະະ\"},\"mapcodes\":[{\"mapcode\":\"JL0.KP\",\"mapcodeInAlphabet\":\"ຍທ0.ດຜ\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"ທຽຫ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"ມ8ມບ.07ຯ\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"ທຽຫ\"},{\"mapcode\":\"SQB.NR3\",\"mapcodeInAlphabet\":\"ຢພກ.ບມ3\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"ກໃທ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"ມ8ມບ.07ຯ\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"ກໃທ\"},{\"mapcode\":\"0L46.LG9\",\"mapcodeInAlphabet\":\"0ທ46.ທຈ9\",\"territory\":\"DEU\",\"territoryInAlphabet\":\"ຄໃຽ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"ມ8ມບ.07ຯ\",\"territory\":\"FRA\",\"territoryInAlphabet\":\"ງມະ\"},{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"ວຍ0ທ6.9ຜບພ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"ະະະ\"}]}",
            "{\"international\":{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"ՏԽ0Հ6.9ՇՃՈ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"ՖՖՖ\"},\"mapcodes\":[{\"mapcode\":\"JL0.KP\",\"mapcodeInAlphabet\":\"ԽՀ0.ԿՇ\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"ՀՅՑ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"Պ8ՊՃ.07Փ\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"ՀՅՑ\"},{\"mapcode\":\"SQB.NR3\",\"mapcodeInAlphabet\":\"ՍՈԲ.ՃՊ3\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"ԲԵՀ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"Պ8ՊՃ.07Փ\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"ԲԵՀ\"},{\"mapcode\":\"0L46.LG9\",\"mapcodeInAlphabet\":\"0Հ46.ՀԹ9\",\"territory\":\"DEU\",\"territoryInAlphabet\":\"ԴԵՅ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"Պ8ՊՃ.07Փ\",\"territory\":\"FRA\",\"territoryInAlphabet\":\"ԸՊՖ\"},{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"ՏԽ0Հ6.9ՇՃՈ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"ՖՖՖ\"}]}",
            "{\"international\":{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"বঝ0ড6.9তণথ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"অঅঅ\"},\"mapcodes\":[{\"mapcode\":\"JL0.KP\",\"mapcodeInAlphabet\":\"ঝড0.ঠত\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"ডওয\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"দ8দণ.07হ\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"ডওয\"},{\"mapcode\":\"SQB.NR3\",\"mapcodeInAlphabet\":\"নথঌ.ণদ3\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"ঌএড\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"দ8দণ.07হ\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"ঌএড\"},{\"mapcode\":\"0L46.LG9\",\"mapcodeInAlphabet\":\"0ড46.ডঙ9\",\"territory\":\"DEU\",\"territoryInAlphabet\":\"খএও\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"দ8দণ.07হ\",\"territory\":\"FRA\",\"territoryInAlphabet\":\"গদঅ\"},{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"বঝ0ড6.9তণথ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"অঅঅ\"}]}",
            "{\"international\":{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"ਲਠ0ਤ6.9ਨਧਪ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"ਅਅਅ\"},\"mapcodes\":[{\"mapcode\":\"JL0.KP\",\"mapcodeInAlphabet\":\"ਠਤ0.ਣਨ\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"ਤਫਸ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"ਭ8ਭਧ.07ਡ\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"ਤਫਸ\"},{\"mapcode\":\"SQB.NR3\",\"mapcodeInAlphabet\":\"ਮਪਕ.ਧਭ3\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"ਕਏਤ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"ਭ8ਭਧ.07ਡ\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"ਕਏਤ\"},{\"mapcode\":\"0L46.LG9\",\"mapcodeInAlphabet\":\"0ਤ46.ਤਜ9\",\"territory\":\"DEU\",\"territoryInAlphabet\":\"ਘਏਫ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"ਭ8ਭਧ.07ਡ\",\"territory\":\"FRA\",\"territoryInAlphabet\":\"ਚਭਅ\"},{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"ਲਠ0ਤ6.9ਨਧਪ\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"ਅਅਅ\"}]}",
            "{\"international\":{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"ཟཇ0ཌ6.9དཏན\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"མམམ\"},\"mapcodes\":[{\"mapcode\":\"JL0.KP\",\"mapcodeInAlphabet\":\"ཇཌ0.ཊད\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"ཌཥར\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"པ8པཏ.07ས\",\"territory\":\"LUX\",\"territoryInAlphabet\":\"ཌཥར\"},{\"mapcode\":\"SQB.NR3\",\"mapcodeInAlphabet\":\"བནཀ.ཏཔ3\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"ཀཤཌ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"པ8པཏ.07ས\",\"territory\":\"BEL\",\"territoryInAlphabet\":\"ཀཤཌ\"},{\"mapcode\":\"0L46.LG9\",\"mapcodeInAlphabet\":\"0ཌ46.ཌཅ9\",\"territory\":\"DEU\",\"territoryInAlphabet\":\"གཤཥ\"},{\"mapcode\":\"R8RN.07Z\",\"mapcodeInAlphabet\":\"པ8པཏ.07ས\",\"territory\":\"FRA\",\"territoryInAlphabet\":\"ངཔམ\"},{\"mapcode\":\"VJ0L6.9PNQ\",\"mapcodeInAlphabet\":\"ཟཇ0ཌ6.9དཏན\",\"territory\":\"AAA\",\"territoryInAlphabet\":\"མམམ\"}]}",
    };
    private final static String[] EXPECTED_ALPHABETS_XML = {
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcodes><international><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>VJ0L6.9PNQ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>AAA</territoryInAlphabet></international><mapcodes><mapcode><mapcode>JL0.KP</mapcode><mapcodeInAlphabet>JL0.KP</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>LUX</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>R8RN.07Z</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>LUX</territoryInAlphabet></mapcode><mapcode><mapcode>SQB.NR3</mapcode><mapcodeInAlphabet>SQB.NR3</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>BEL</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>R8RN.07Z</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>BEL</territoryInAlphabet></mapcode><mapcode><mapcode>0L46.LG9</mapcode><mapcodeInAlphabet>0L46.LG9</mapcodeInAlphabet><territory>DEU</territory><territoryInAlphabet>DEU</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>R8RN.07Z</mapcodeInAlphabet><territory>FRA</territory><territoryInAlphabet>FRA</territoryInAlphabet></mapcode><mapcode><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>VJ0L6.9PNQ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>AAA</territoryInAlphabet></mapcode></mapcodes></mapcodes>",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcodes><international><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>ΦΠ0Λ6.9ΡΝΘ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>ΑΑΑ</territoryInAlphabet></international><mapcodes><mapcode><mapcode>JL0.KP</mapcode><mapcodeInAlphabet>ΠΛ0.ΚΡ</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>LUX</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>Ψ8ΨΝ.07Ζ</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>LUX</territoryInAlphabet></mapcode><mapcode><mapcode>SQB.NR3</mapcode><mapcodeInAlphabet>ΣΘΒ.ΝΨ3</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>BEL</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>Ψ8ΨΝ.07Ζ</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>BEL</territoryInAlphabet></mapcode><mapcode><mapcode>0L46.LG9</mapcode><mapcodeInAlphabet>0Λ46.ΛΓ9</mapcodeInAlphabet><territory>DEU</territory><territoryInAlphabet>DEU</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>Ψ8ΨΝ.07Ζ</mapcodeInAlphabet><territory>FRA</territory><territoryInAlphabet>ΕΨΑ</territoryInAlphabet></mapcode><mapcode><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>ΦΠ0Λ6.9ΡΝΘ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>ΑΑΑ</territoryInAlphabet></mapcode></mapcodes></mapcodes>",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcodes><international><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>ЧП0Л6.9РЗФ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>ААА</territoryInAlphabet></international><mapcodes><mapcode><mapcode>JL0.KP</mapcode><mapcodeInAlphabet>ПЛ0.КР</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>ЛЭХ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>Я8ЯЗ.07Б</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>ЛЭХ</territoryInAlphabet></mapcode><mapcode><mapcode>SQB.NR3</mapcode><mapcodeInAlphabet>ЦФВ.ЗЯ3</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>ВЕЛ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>Я8ЯЗ.07Б</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>ВЕЛ</territoryInAlphabet></mapcode><mapcode><mapcode>0L46.LG9</mapcode><mapcodeInAlphabet>0Л46.ЛГ9</mapcodeInAlphabet><territory>DEU</territory><territoryInAlphabet>ДЕЭ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>Я8ЯЗ.07Б</mapcodeInAlphabet><territory>FRA</territory><territoryInAlphabet>ЖЯА</territoryInAlphabet></mapcode><mapcode><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>ЧП0Л6.9РЗФ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>ААА</territoryInAlphabet></mapcode></mapcodes></mapcodes>",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcodes><international><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>צט0ך6.9םלמ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>אאא</territoryInAlphabet></international><mapcodes><mapcode><mapcode>JL0.KP</mapcode><mapcodeInAlphabet>טך0.ים</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>ךץר</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>נ8נל.07ת</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>ךץר</territoryInAlphabet></mapcode><mapcode><mapcode>SQB.NR3</mapcode><mapcodeInAlphabet>עמב.לנ3</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>בףך</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>נ8נל.07ת</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>בףך</territoryInAlphabet></mapcode><mapcode><mapcode>0L46.LG9</mapcode><mapcodeInAlphabet>0ך46.ךז9</mapcodeInAlphabet><territory>DEU</territory><territoryInAlphabet>דףץ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>נ8נל.07ת</mapcodeInAlphabet><territory>FRA</territory><territoryInAlphabet>הנא</territoryInAlphabet></mapcode><mapcode><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>צט0ך6.9םלמ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>אאא</territoryInAlphabet></mapcode></mapcodes></mapcodes>",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcodes><international><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>लठ0त6.9नधप</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>अअअ</territoryInAlphabet></international><mapcodes><mapcode><mapcode>JL0.KP</mapcode><mapcodeInAlphabet>ठत0.णन</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>तफस</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>भ8भध.07ड</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>तफस</territoryInAlphabet></mapcode><mapcode><mapcode>SQB.NR3</mapcode><mapcodeInAlphabet>मपक.धभ3</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>कएत</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>भ8भध.07ड</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>कएत</territoryInAlphabet></mapcode><mapcode><mapcode>0L46.LG9</mapcode><mapcodeInAlphabet>0त46.तज9</mapcodeInAlphabet><territory>DEU</territory><territoryInAlphabet>घएफ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>भ8भध.07ड</mapcodeInAlphabet><territory>FRA</territory><territoryInAlphabet>चभअ</territoryInAlphabet></mapcode><mapcode><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>लठ0त6.9नधप</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>अअअ</territoryInAlphabet></mapcode></mapcodes></mapcodes>",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcodes><international><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>ഴഡ0ഥ6.9നധമ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>ഒഒഒ</territoryInAlphabet></international><mapcodes><mapcode><mapcode>JL0.KP</mapcode><mapcodeInAlphabet>ഡഥ0.തന</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>ഥഉശ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>ര8രധ.07ഹ</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>ഥഉശ</territoryInAlphabet></mapcode><mapcode><mapcode>SQB.NR3</mapcode><mapcodeInAlphabet>റമക.ധര3</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>കഋഥ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>ര8രധ.07ഹ</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>കഋഥ</territoryInAlphabet></mapcode><mapcode><mapcode>0L46.LG9</mapcode><mapcodeInAlphabet>0ഥ46.ഥജ9</mapcodeInAlphabet><territory>DEU</territory><territoryInAlphabet>ഗഋഉ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>ര8രധ.07ഹ</mapcodeInAlphabet><territory>FRA</territory><territoryInAlphabet>ചരഒ</territoryInAlphabet></mapcode><mapcode><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>ഴഡ0ഥ6.9നധമ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>ഒഒഒ</territoryInAlphabet></mapcode></mapcodes></mapcodes>",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcodes><international><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>ႺႮ0Ⴑ6.9ႵႴႶ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>ႠႠႠ</territoryInAlphabet></international><mapcodes><mapcode><mapcode>JL0.KP</mapcode><mapcodeInAlphabet>ႮႱ0.ႰႵ</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>ႱႨႽ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>Ⴗ8ႷႴ.07Ⴟ</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>ႱႨႽ</territoryInAlphabet></mapcode><mapcode><mapcode>SQB.NR3</mapcode><mapcodeInAlphabet>ႸႶႡ.ႴႷ3</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>ႡႤႱ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>Ⴗ8ႷႴ.07Ⴟ</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>ႡႤႱ</territoryInAlphabet></mapcode><mapcode><mapcode>0L46.LG9</mapcode><mapcodeInAlphabet>0Ⴑ46.ႱႫ9</mapcodeInAlphabet><territory>DEU</territory><territoryInAlphabet>ႦႤႨ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>Ⴗ8ႷႴ.07Ⴟ</mapcodeInAlphabet><territory>FRA</territory><territoryInAlphabet>ႩႷႠ</territoryInAlphabet></mapcode><mapcode><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>ႺႮ0Ⴑ6.9ႵႴႶ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>ႠႠႠ</territoryInAlphabet></mapcode></mapcodes></mapcodes>",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcodes><international><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>モス0ト6.9ヒヌフ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>アアア</territoryInAlphabet></international><mapcodes><mapcode><mapcode>JL0.KP</mapcode><mapcodeInAlphabet>スト0.チヒ</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>トエラ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>ヘ8ヘヌ.07ヲ</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>トエラ</territoryInAlphabet></mapcode><mapcode><mapcode>SQB.NR3</mapcode><mapcodeInAlphabet>ホフカ.ヌヘ3</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>カオト</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>ヘ8ヘヌ.07ヲ</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>カオト</territoryInAlphabet></mapcode><mapcode><mapcode>0L46.LG9</mapcode><mapcodeInAlphabet>0ト46.トコ9</mapcodeInAlphabet><territory>DEU</territory><territoryInAlphabet>クオエ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>ヘ8ヘヌ.07ヲ</mapcodeInAlphabet><territory>FRA</territory><territoryInAlphabet>ケヘア</territoryInAlphabet></mapcode><mapcode><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>モス0ト6.9ヒヌフ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>アアア</territoryInAlphabet></mapcode></mapcodes></mapcodes>",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcodes><international><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>ลช0ด6.9ธทบ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>ะะะ</territoryInAlphabet></international><mapcodes><mapcode><mapcode>JL0.KP</mapcode><mapcodeInAlphabet>ชด0.ฑธ</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>ดฬอ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>ผ8ผท.07ฯ</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>ดฬอ</territoryInAlphabet></mapcode><mapcode><mapcode>SQB.NR3</mapcode><mapcodeInAlphabet>มบก.ทผ3</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>กาด</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>ผ8ผท.07ฯ</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>กาด</territoryInAlphabet></mapcode><mapcode><mapcode>0L46.LG9</mapcode><mapcodeInAlphabet>0ด46.ดจ9</mapcodeInAlphabet><territory>DEU</territory><territoryInAlphabet>คาฬ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>ผ8ผท.07ฯ</mapcodeInAlphabet><territory>FRA</territory><territoryInAlphabet>งผะ</territoryInAlphabet></mapcode><mapcode><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>ลช0ด6.9ธทบ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>ะะะ</territoryInAlphabet></mapcode></mapcodes></mapcodes>",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcodes><international><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>ວຍ0ທ6.9ຜບພ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>ະະະ</territoryInAlphabet></international><mapcodes><mapcode><mapcode>JL0.KP</mapcode><mapcodeInAlphabet>ຍທ0.ດຜ</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>ທຽຫ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>ມ8ມບ.07ຯ</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>ທຽຫ</territoryInAlphabet></mapcode><mapcode><mapcode>SQB.NR3</mapcode><mapcodeInAlphabet>ຢພກ.ບມ3</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>ກໃທ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>ມ8ມບ.07ຯ</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>ກໃທ</territoryInAlphabet></mapcode><mapcode><mapcode>0L46.LG9</mapcode><mapcodeInAlphabet>0ທ46.ທຈ9</mapcodeInAlphabet><territory>DEU</territory><territoryInAlphabet>ຄໃຽ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>ມ8ມບ.07ຯ</mapcodeInAlphabet><territory>FRA</territory><territoryInAlphabet>ງມະ</territoryInAlphabet></mapcode><mapcode><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>ວຍ0ທ6.9ຜບພ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>ະະະ</territoryInAlphabet></mapcode></mapcodes></mapcodes>",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcodes><international><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>ՏԽ0Հ6.9ՇՃՈ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>ՖՖՖ</territoryInAlphabet></international><mapcodes><mapcode><mapcode>JL0.KP</mapcode><mapcodeInAlphabet>ԽՀ0.ԿՇ</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>ՀՅՑ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>Պ8ՊՃ.07Փ</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>ՀՅՑ</territoryInAlphabet></mapcode><mapcode><mapcode>SQB.NR3</mapcode><mapcodeInAlphabet>ՍՈԲ.ՃՊ3</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>ԲԵՀ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>Պ8ՊՃ.07Փ</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>ԲԵՀ</territoryInAlphabet></mapcode><mapcode><mapcode>0L46.LG9</mapcode><mapcodeInAlphabet>0Հ46.ՀԹ9</mapcodeInAlphabet><territory>DEU</territory><territoryInAlphabet>ԴԵՅ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>Պ8ՊՃ.07Փ</mapcodeInAlphabet><territory>FRA</territory><territoryInAlphabet>ԸՊՖ</territoryInAlphabet></mapcode><mapcode><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>ՏԽ0Հ6.9ՇՃՈ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>ՖՖՖ</territoryInAlphabet></mapcode></mapcodes></mapcodes>",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcodes><international><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>বঝ0ড6.9তণথ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>অঅঅ</territoryInAlphabet></international><mapcodes><mapcode><mapcode>JL0.KP</mapcode><mapcodeInAlphabet>ঝড0.ঠত</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>ডওয</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>দ8দণ.07হ</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>ডওয</territoryInAlphabet></mapcode><mapcode><mapcode>SQB.NR3</mapcode><mapcodeInAlphabet>নথঌ.ণদ3</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>ঌএড</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>দ8দণ.07হ</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>ঌএড</territoryInAlphabet></mapcode><mapcode><mapcode>0L46.LG9</mapcode><mapcodeInAlphabet>0ড46.ডঙ9</mapcodeInAlphabet><territory>DEU</territory><territoryInAlphabet>খএও</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>দ8দণ.07হ</mapcodeInAlphabet><territory>FRA</territory><territoryInAlphabet>গদঅ</territoryInAlphabet></mapcode><mapcode><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>বঝ0ড6.9তণথ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>অঅঅ</territoryInAlphabet></mapcode></mapcodes></mapcodes>",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcodes><international><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>ਲਠ0ਤ6.9ਨਧਪ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>ਅਅਅ</territoryInAlphabet></international><mapcodes><mapcode><mapcode>JL0.KP</mapcode><mapcodeInAlphabet>ਠਤ0.ਣਨ</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>ਤਫਸ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>ਭ8ਭਧ.07ਡ</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>ਤਫਸ</territoryInAlphabet></mapcode><mapcode><mapcode>SQB.NR3</mapcode><mapcodeInAlphabet>ਮਪਕ.ਧਭ3</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>ਕਏਤ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>ਭ8ਭਧ.07ਡ</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>ਕਏਤ</territoryInAlphabet></mapcode><mapcode><mapcode>0L46.LG9</mapcode><mapcodeInAlphabet>0ਤ46.ਤਜ9</mapcodeInAlphabet><territory>DEU</territory><territoryInAlphabet>ਘਏਫ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>ਭ8ਭਧ.07ਡ</mapcodeInAlphabet><territory>FRA</territory><territoryInAlphabet>ਚਭਅ</territoryInAlphabet></mapcode><mapcode><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>ਲਠ0ਤ6.9ਨਧਪ</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>ਅਅਅ</territoryInAlphabet></mapcode></mapcodes></mapcodes>",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mapcodes><international><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>ཟཇ0ཌ6.9དཏན</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>མམམ</territoryInAlphabet></international><mapcodes><mapcode><mapcode>JL0.KP</mapcode><mapcodeInAlphabet>ཇཌ0.ཊད</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>ཌཥར</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>པ8པཏ.07ས</mapcodeInAlphabet><territory>LUX</territory><territoryInAlphabet>ཌཥར</territoryInAlphabet></mapcode><mapcode><mapcode>SQB.NR3</mapcode><mapcodeInAlphabet>བནཀ.ཏཔ3</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>ཀཤཌ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>པ8པཏ.07ས</mapcodeInAlphabet><territory>BEL</territory><territoryInAlphabet>ཀཤཌ</territoryInAlphabet></mapcode><mapcode><mapcode>0L46.LG9</mapcode><mapcodeInAlphabet>0ཌ46.ཌཅ9</mapcodeInAlphabet><territory>DEU</territory><territoryInAlphabet>གཤཥ</territoryInAlphabet></mapcode><mapcode><mapcode>R8RN.07Z</mapcode><mapcodeInAlphabet>པ8པཏ.07ས</mapcodeInAlphabet><territory>FRA</territory><territoryInAlphabet>ངཔམ</territoryInAlphabet></mapcode><mapcode><mapcode>VJ0L6.9PNQ</mapcode><mapcodeInAlphabet>ཟཇ0ཌ6.9དཏན</mapcodeInAlphabet><territory>AAA</territory><territoryInAlphabet>མམམ</territoryInAlphabet></mapcode></mapcodes></mapcodes>"
    };
}
