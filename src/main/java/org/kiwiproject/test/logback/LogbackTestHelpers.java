package org.kiwiproject.test.logback;

import com.google.common.io.Resources;

import lombok.experimental.UtilityClass;

import ch.qos.logback.classic.ClassicConstants;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

import org.slf4j.LoggerFactory;

/**
 * Static test utilities that provide Logback-related functionality.
 */
@UtilityClass
public class LogbackTestHelpers {

    /**
     * Reset the Logback logging system using the default test configuration file ({@code logback-test.xml}).
     *
     * @see ClassicConstants#TEST_AUTOCONFIG_FILE
     * @throws UncheckedJoranException if an error occurs resetting Logback
     */
    public static void resetLogback() {
        resetLogback(ClassicConstants.TEST_AUTOCONFIG_FILE);
    }

    /**
     * Reset the Logback logging system using the given configuration file.
     *
     * @param logbackConfigFilePath the location of the custom Logback configuration file
     * @throws UncheckedJoranException if an error occurs resetting Logback
     */
    public static void resetLogback(String logbackConfigFilePath) {
        try {
            var loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            loggerContext.stop();

            var joranConfigurator = new JoranConfigurator();
            joranConfigurator.setContext(loggerContext);
            var logbackConfigUrl = Resources.getResource(logbackConfigFilePath);
            joranConfigurator.doConfigure(logbackConfigUrl);
            loggerContext.start();
        } catch (JoranException e) {
            throw new UncheckedJoranException(e);
        }
    }
}
