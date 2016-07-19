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

package com.mapcode.services.standalone;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.mapcode.services.ResourcesModule;
import com.tomtom.speedtools.guice.GuiceConfigurationModule;
import com.tomtom.speedtools.rest.ServicesModule;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

import javax.annotation.Nonnull;

@SuppressWarnings({"UseOfSystemOutOrSystemErr", "ConstantConditions"})
public class MainCommandLine {
    private static final String CMD_HELP = "--help";
    private static final String CMD_SILENT = "--silent";
    private static final String CMD_DEBUG = "--debug";
    private static final String CMD_PORT = "--port";

    private static final int DEFAULT_PORT = 8080;

    private MainCommandLine() {
        // Prevent instantiation.
    }

    public static void execute(final String[] args) {
        int port = DEFAULT_PORT;
        String command = null;
        boolean debug = false;

        // Configure log4j.
        final Logger rootLogger = Logger.getRootLogger();
        rootLogger.setLevel(Level.INFO);

        final ConsoleAppender consoleAppender = new ConsoleAppender(new SimpleLayout());
        consoleAppender.setThreshold(Level.INFO);
        rootLogger.addAppender(consoleAppender);

        // Parse command-line arguments.
        int index = 0;
        while (index < args.length) {

            switch (args[index]) {
                case CMD_SILENT:
                    rootLogger.setLevel(Level.WARN);
                    consoleAppender.setThreshold(Level.WARN);
                    break;

                case CMD_DEBUG:
                    debug = true;
                    break;

                case CMD_PORT:
                    if (index >= args.length) {
                        System.out.println("Missing port number");
                        printUsage();
                        return;
                    }
                    port = Integer.parseInt(args[index + 1]);
                    ++index;
                    break;

                default:
                    if (args[index].startsWith("-")) {
                        System.out.println("Unknown option: " + args[index]);
                        printUsage();
                        return;
                    }
                    if (command != null) {
                        System.out.println("Unknown argument: " + args[index]);
                        printUsage();
                        return;
                    }
                    command = args[index];
                    break;
            }
            ++index;
        }

        if (debug) {
            consoleAppender.setThreshold(Level.DEBUG);
        }

        if ((command != null) && command.equals(CMD_HELP)) {
            printUsage();
        } else {
            final Injector guice = createGuice();
            final Server server = guice.getInstance(Server.class);
            server.startServer(port);
        }
    }

    /**
     * Create the guice injector.
     *
     * @return Guice injector.
     */
    private static Injector createGuice() {
        return Guice.createInjector(
                new GuiceConfigurationModule(
                        "classpath:speedtools.default.properties",      // Default set required by SpeedTools.
                        "classpath:mapcode.properties",                 // Specific for mapcode service.
                        "classpath:mapcode-notrace.properties"),        // No tracing.
                new ServicesModule(),
                new ResourcesModule(),
                new StandaloneModule());
    }

    private static void printUsage() {
        System.out.println("Usage: java -jar <warfile>" + " [" + CMD_PORT + " <port>] [" + CMD_SILENT + "] [" + CMD_DEBUG + ']');
        System.out.println("       java -jar <warfile> " + CMD_HELP);
    }

    private static void endNotice(@Nonnull final String filename) {
        assert filename != null;
        System.out.println("(Note: the full output of this program is stored in: " + filename);
        System.out.println("This file may grow very large. You may wish to remove it.)");
    }
}
