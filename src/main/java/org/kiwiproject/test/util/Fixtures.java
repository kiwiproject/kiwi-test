package org.kiwiproject.test.util;

import static java.util.Objects.nonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Resources;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.kiwiproject.net.UncheckedURISyntaxException;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Helper methods for working with test fixtures.
 */
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
     * Reads the given fixture file from the classpath (e.g. {@code src/test/resources})
     * and returns its contents as a string.
     *
     * @param resourceName the name/path of to the classpath resource
     * @param charset      the charset of the fixture file
     * @return the fixture contents
     * @throws UncheckedURISyntaxException if the resource name/path is invalid as a URI
     * @throws UncheckedIOException        if an I/O error occurs; the cause will be the actual {@link IOException}
     * @implNote As of Java 17, {@link Files#readString(Path, Charset)} throws an {@link Error} instead of
     * an {@link IOException} in the case of malformed input or unmappable characters. This {@link Error} contains a cause
     * of {@link IOException}, with actual type of either {@link java.nio.charset.MalformedInputException MalformedInputException}
     * or {@link java.nio.charset.UnmappableCharacterException UnmappableCharacterException}, both of which are subclasses of
     * {@link CharacterCodingException}. Before Java 17 (we've tested on 11 and 16), this method threw an
     * {@link IOException} whose actual type was one of the previously mentioned {@link CharacterCodingException}
     * subclasses. This could be a bug in the JDK, or it could be a misunderstanding of how
     * {@link Files#readString(Path, Charset)} is supposed to work. However, its Javadoc clearly states that it throws
     * an {@link IOException} "if an I/O error occurs reading from the file or a malformed or unmappable byte
     * sequence is read". In our tests of malformed input and unmappable characters, we see the {@link Error} thrown
     * instead of a {@link CharacterCodingException} on JDKs prior to 17. As a result, we have included code to handle
     * the specific case when an {@link Error} is thrown and its cause is a {@link CharacterCodingException}.
     */
    @SneakyThrows
    @SuppressWarnings("java:S1181")  // Suppress Sonar "Throwable and Error should not be caught" warning (see implNote)
    public static String fixture(String resourceName, Charset charset) {
        try {
            var url = Resources.getResource(resourceName);
            var path = pathFromURL(url);
            return Files.readString(path, charset);
        } catch (IOException e) {
            throw new UncheckedIOException("Error reading fixture: " + resourceName, e);
        } catch (Error error) {
            // Special handling for JDK 17+ (see implementation note above). Also, this relies on the "SneakyThrows".
            throw uncheckedIOExceptionOrOriginalError(error, resourceName);
        }
    }

    /**
     * This method only exists to permit testing it when building on JDK 11-16, since those throw UncheckedIOException
     * whereas JDK 17 throws an Error (see above implNote in {@link #fixture(String, Charset)}).
     *
     * @param error        the original Error
     * @param resourceName the resource name, only used when UncheckedIOException is returned
     * @return the original {@code error} or a new UncheckedIOException
     */
    @VisibleForTesting
    static Throwable uncheckedIOExceptionOrOriginalError(Error error, String resourceName) {
        var cause = error.getCause();
        if (nonNull(cause) && cause instanceof CharacterCodingException) {
            return new UncheckedIOException("Error reading fixture: " + resourceName, (CharacterCodingException) cause);
        }
        return error;
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
