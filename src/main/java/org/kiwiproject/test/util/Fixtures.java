package org.kiwiproject.test.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Resources;
import lombok.experimental.UtilityClass;
import org.kiwiproject.net.UncheckedURISyntaxException;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Helper methods for working with test fixtures.
 */
@SuppressWarnings("UnstableApiUsage")
@UtilityClass
public class Fixtures {

    /**
     * Reads the given fixture file from the classpath (e. g. {@code src/test/resources}) and returns its contents
     * as a UTF-8 string.
     *
     * @param resourceName the name/path of to the classpath resource
     * @return the fixture contents
     * @throws UncheckedURISyntaxException if the resource name/path is invalid as a URI
     * @throws UncheckedIOException        if an I/O error occurs
     */
    public static String fixture(String resourceName) {
        return fixture(resourceName, StandardCharsets.UTF_8);
    }

    /**
     * Reads the given fixture file from the classpath (e. g. {@code src/test/resources})
     * and returns its contents as a string.
     *
     * @param resourceName the name/path of to the classpath resource
     * @param charset      the charset of the fixture file
     * @return the fixture contents
     * @throws UncheckedURISyntaxException if the resource name/path is invalid as a URI
     * @throws UncheckedIOException        if an I/O error occurs
     */
    public static String fixture(String resourceName, Charset charset) {
        try {
            var url = Resources.getResource(resourceName);
            var path = pathFromURL(url);
            return Files.readString(path, charset);
        } catch (IOException e) {
            throw new UncheckedIOException("Error reading fixture: " + resourceName, e);
        }
    }

    /**
     * Resolves the given fixture file name/path as a {@link File}.
     *
     * @param resourceName the name/path of to the classpath resource
     * @return the resource as a {@link File}
     * @throws UncheckedURISyntaxException if the resource name/path is invalid as a URI
     */
    public static File fixtureFile(String resourceName) {
        return fixturePath(resourceName).toFile();
    }

    /**
     * Resolves the given fixture file name/path as a {@link Path}.
     *
     * @param resourceName the name/path of to the classpath resource
     * @return the resource as a {@link Path}
     * @throws UncheckedURISyntaxException if the resource name/path is invalid
     */
    public static Path fixturePath(String resourceName) {
        var url = Resources.getResource(resourceName);
        return pathFromURL(url);
    }

    /**
     * @implNote In reality this should never throw, since {@link Resources#getResource(String)} uses
     * {@link ClassLoader#getResource(String)} under the covers, and that method escapes invalid characters from what
     * I have seen and tried. For example, {@code file-with>invalid-character.txt} is actually escaped as
     * {@code file-with%3einvalid-character.txt} which converts to a URI just fine.
     */
    @VisibleForTesting
    static Path pathFromURL(URL url) {
        try {
            return Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            throw new UncheckedURISyntaxException("Error getting path from URL: " + url, e);
        }
    }
}
