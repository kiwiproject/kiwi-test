package org.kiwiproject.test.logback;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;
import static org.kiwiproject.base.KiwiPreconditions.checkArgumentNotBlank;
import static org.kiwiproject.base.KiwiPreconditions.checkArgumentNotNull;

import ch.qos.logback.classic.ClassicConstants;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import com.google.common.io.Resources;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Static test utilities that provide Logback-related functionality.
 */
@UtilityClass
public class LogbackTestHelpers {

    /**
     * Reset the Logback logging system using the default test configuration file ({@code logback-test.xml}).
     * If that doesn't exist, try to fall back to the default configuration file ({@code logback.xml}).
     * <p>
     * If you need a custom location (or locations), use {@link #resetLogback(String, String...)}.
     *
     * @throws UncheckedJoranException if an error occurs resetting Logback
     * @see ClassicConstants#TEST_AUTOCONFIG_FILE
     * @see ClassicConstants#AUTOCONFIG_FILE
     */
    public static void resetLogback() {
        resetLogback(ClassicConstants.TEST_AUTOCONFIG_FILE, ClassicConstants.AUTOCONFIG_FILE);
    }

    /**
     * Reset the Logback logging system using the given configuration file.
     * If the primary file does not exist, use the first fallback configuration
     * file that exists. If the reset fails, an exception is thrown immediately.
     * <p>
     * The fallback configurations are searched in the order they are provided.
     *
     * @param logbackConfigFile the location of the custom Logback configuration file
     * @param fallbackConfigFiles additional locations to check for Logback configuration files
     * @throws UncheckedJoranException if an error occurs resetting Logback
     * @throws IllegalArgumentException if none of the Logback configuration files exist
     */
    public static void resetLogback(String logbackConfigFile, String... fallbackConfigFiles) {
        checkArgumentNotBlank(logbackConfigFile, "logbackConfigFile must not be blank");
        checkArgumentNotNull(fallbackConfigFiles, "fallback locations vararg parameter must not be null");
        checkArgument(isNoneBlank(fallbackConfigFiles), "fallbackConfigFiles must not contain blank locations");

        try {
            var logbackConfigUrl = getFirstLogbackConfigOrThrow(logbackConfigFile, fallbackConfigFiles);

            var loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            loggerContext.stop();

            var joranConfigurator = new JoranConfigurator();
            joranConfigurator.setContext(loggerContext);
            joranConfigurator.doConfigure(logbackConfigUrl);
            loggerContext.start();
        } catch (JoranException e) {
            throw new UncheckedJoranException(e);
        }
    }

    private static URL getFirstLogbackConfigOrThrow(String logbackConfigFilePath, String... fallbackConfigFilePaths) {
        var allConfigs = Stream.concat(
                Stream.of(logbackConfigFilePath),
                Arrays.stream(fallbackConfigFilePaths)
        ).toList();

        return allConfigs.stream()
                .map(LogbackTestHelpers::getResourceOrNull)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Did not find any of the Logback configurations: " + allConfigs));
    }

    @Nullable
    private static URL getResourceOrNull(String resourceName) {
        try {
            return Resources.getResource(resourceName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
