package org.kiwiproject.test.logback;

import static org.apache.commons.lang3.StringUtils.isBlank;

import org.jspecify.annotations.Nullable;

/**
 * Provides utilities for Logback-related functionality.
 * <p>
 * This is an instance-based utility class, and is mainly useful if you need to mock
 * its behavior. By default, it delegates to {@link LogbackTestHelpers} for methods
 * that have the same signature.
 */
public class LogbackTestHelper {

    /**
     * Resets Logback using either the given config file, or uses the defaults
     * as provided by {@link LogbackTestHelpers#resetLogback()}.
     *
     * @param logbackConfigFile the Logback config file to use, or null
     */
    public void resetLogbackWithDefaultOrConfig(@Nullable String logbackConfigFile) {
        if (isBlank(logbackConfigFile)) {
            resetLogback();
        } else {
            resetLogback(logbackConfigFile);
        }
    }

    /**
     * Delegates to {@link LogbackTestHelpers#resetLogback()}.
     */
    public void resetLogback() {
        LogbackTestHelpers.resetLogback();
    }

    /**
     * Delegates to {@link LogbackTestHelpers#resetLogback(String, String...)}.
     *
     * @param logbackConfigFile   the location of the custom Logback configuration file
     * @param fallbackConfigFiles additional locations to check for Logback configuration files
     */
    public void resetLogback(String logbackConfigFile, String... fallbackConfigFiles) {
        LogbackTestHelpers.resetLogback(logbackConfigFile, fallbackConfigFiles);
    }
}
