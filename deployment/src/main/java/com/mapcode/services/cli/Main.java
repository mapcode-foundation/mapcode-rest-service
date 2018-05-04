/*
 * Copyright (C) 2016-2018, Stichting Mapcode Foundation (http://www.mapcode.com)
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
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
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

    @SuppressWarnings("OverlyBroadThrowsClause")
    public static void main(@Nonnull final String[] args)
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IOException {
        assert args != null;
        final List<URL> newUrls = new ArrayList<>();
        URL.setURLStreamHandlerFactory(new NestedJarURLStreamHandlerFactory());
        final String warFile = getWarFile();
        try (final JarFile jarFile = new JarFile(warFile)) {

            //noinspection ForLoopWithMissingComponent
            for (final Enumeration<JarEntry> entryEnum = jarFile.entries(); entryEnum.hasMoreElements(); ) {
                final JarEntry entry = entryEnum.nextElement();
                if (entry.getName().endsWith(".jar")) {
                    newUrls.add(new URL("jar:nestedjar:file:" + warFile + "~/" + entry.getName() + "!/"));
                }
            }
        }

        final URLClassLoader newClassLoader = new URLClassLoader(newUrls.toArray(new URL[newUrls.size()]));
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
        final URLClassLoader classLoader = (URLClassLoader) Main.class.getClassLoader();
        for (final URL url : classLoader.getURLs()) {
            if (url.toString().endsWith(".war")) {
                return url.getFile();
            }
        }
        throw new IllegalStateException("Should be run from the war file (using java -jar <warfile>).");
    }

    static class NestedJarURLConnection extends URLConnection {
        private final URLConnection connection;
        final static char SEPARATOR_CHAR = '~';
        final static String SEPARATOR = SEPARATOR_CHAR + "/";

        @SuppressWarnings("OverlyBroadThrowsClause")
        NestedJarURLConnection(@Nonnull final URL url)
                throws IOException {
            super(url);
            assert url != null;
            connection = new URL(url.getFile()).openConnection();
        }

        @Override
        public void connect() throws IOException {
            if (!connected) {
                connection.connect();
                connected = true;
            }
        }

        @Override
        @Nonnull
        public InputStream getInputStream() throws IOException {
            connect();
            return connection.getInputStream();
        }
    }

    static class NestedJarURLStreamHandlerFactory implements URLStreamHandlerFactory {

        @Nullable
        @Override
        public URLStreamHandler createURLStreamHandler(@Nonnull final String protocol) {
            assert protocol != null;
            if (protocol.equals("nestedjar")) {
                return new JarJarURLStreamHandler();
            }
            return null;
        }
    }

    static class JarJarURLStreamHandler extends URLStreamHandler {

        @SuppressWarnings("DuplicateThrows")
        @Nonnull
        @Override
        protected URLConnection openConnection(@Nonnull final URL u) throws IOException {
            assert u != null;
            return new NestedJarURLConnection(u);
        }

        @Override
        protected void parseURL(@Nonnull final URL u, @Nonnull final String spec, final int start, final int limit) {
            assert u != null;
            assert spec != null;
            final String file = "jar:" + spec.substring(start, limit).replaceFirst("\\~/", "!/");
            setURL(u, "nestedjar", "", -1, null, null, file, null, null);
        }
    }
}
