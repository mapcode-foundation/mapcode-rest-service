/*
 * Copyright (C) 2016-2020, Stichting Mapcode Foundation (http://www.mapcode.com)
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

package com.mapcode.services.cli;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Stub to load CLI main from war file.
 * You can start the application with the following command:
 *
 * java -jar war-file [arguments]
 *
 */
public final class Main {
    private static final String MAIN_CLASS_NAME = "com.mapcode.services.standalone.MainCommandLine";
    private static final String MAIN_METHOD_NAME = "execute";

    private Main() {
        // Prevent instantiation.
    }

    public static void main(@Nonnull final String... args)
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IOException {
        assert args != null;
        final String warFile = getWarFile();

        // Extract nested JARs from the WAR to a temp directory and build a
        // URLClassLoader from them. The old nestedjar:// URL scheme no longer
        // works on Java 9+ because JarURLConnection became stricter about
        // double-nested URLs.
        final File tempDir = Files.createTempDirectory("mapcode-war-").toFile();
        tempDir.deleteOnExit();

        final List<URL> newUrls = new ArrayList<>();
        try (final JarFile jarFile = new JarFile(warFile)) {
            for (final Enumeration<JarEntry> entryEnum = jarFile.entries(); entryEnum.hasMoreElements(); ) {
                final JarEntry entry = entryEnum.nextElement();
                if (entry.getName().endsWith(".jar")) {
                    final File tempJar = new File(tempDir, new File(entry.getName()).getName());
                    tempJar.deleteOnExit();
                    try (final InputStream is = jarFile.getInputStream(entry)) {
                        Files.copy(is, tempJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                    newUrls.add(tempJar.toURI().toURL());
                }
            }
        }

        final URLClassLoader newClassLoader = new URLClassLoader(newUrls.toArray(new URL[0]));
        Thread.currentThread().setContextClassLoader(newClassLoader);
        final Class<?> mainClass = newClassLoader.loadClass(MAIN_CLASS_NAME);
        final Method method = mainClass.getMethod(MAIN_METHOD_NAME, String[].class);
        try {
            method.invoke(null, (Object) args);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("The program failed. An exception was thrown during execution.",
                    e.getCause());
        }
    }

    @Nonnull
    private static String getWarFile() {
        // In Java 9+, AppClassLoader no longer extends URLClassLoader, so we
        // read the WAR path from the system classpath instead.
        final String classPath = System.getProperty("java.class.path", "");
        final String pathSeparator = System.getProperty("path.separator", ":");
        for (final String entry : classPath.split(pathSeparator)) {
            if (entry.endsWith(".war")) {
                return entry;
            }
        }
        throw new IllegalStateException("Should be run from the war file (using java -jar <warfile>).");
    }
}
