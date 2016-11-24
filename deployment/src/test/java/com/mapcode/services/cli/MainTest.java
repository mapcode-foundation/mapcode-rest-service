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

package com.mapcode.services.cli;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("OverlyBroadThrowsClause")
public class MainTest {
    private static final Logger LOG = LoggerFactory.getLogger(MainTest.class);

    @SuppressWarnings("ProhibitedExceptionDeclared")
    @Test(expected = IllegalStateException.class)
    public void testMainFails() throws Exception {
        LOG.info("testMainFails");

        // This will fail, as it needs to run from a WAR file and we can't do that here.
        // Arguments: [--port <port>] [--silent] [--debug] [--help]
        Main.main(new String[]{"--port", "8080", "--debug"});
    }
}
