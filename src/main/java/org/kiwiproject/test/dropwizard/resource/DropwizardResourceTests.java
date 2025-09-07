package org.kiwiproject.test.dropwizard.resource;

import static org.kiwiproject.base.KiwiPreconditions.checkArgumentNotNull;

import io.dropwizard.testing.junit5.ResourceExtension;
import lombok.experimental.UtilityClass;

/**
 * Test utility for Dropwizard resource test classes using {@link ResourceExtension}.
 */
@UtilityClass
public class DropwizardResourceTests {

    /**
     * Create a {@link ResourceExtension} for the given Jakarta REST {@code resource} instance.
     * <p>
     * This is useful when you don't need complex configuration and only need to
     * create the extension for a single resource instance.
     * <p>
     * <strong>Note:</strong> The returned extension preserves the existing Logback
     * configuration by disabling the logging bootstrap.
     *
     * @param resource the Jakarta REST resource instance
     * @return a new {@link ResourceExtension} instance
     * @see #resourceBuilderPreservingLogbackConfig()
     */
    public static ResourceExtension resourceExtensionFor(Object resource) {
        checkArgumentNotNull(resource, "resource must not be null");
        return resourceBuilderPreservingLogbackConfig()
                .addResource(resource)
                .build();
    }

    /**
     * Create a {@link ResourceExtension} builder that preserves the existing Logback
     * configuration by disabling the logging bootstrap.
     * <p>
     * Tired of writing {@code ResourceExtension.builder().bootstrapLogging(false)}
     * every time you create a {@link ResourceExtension}? Then use this absurdly
     * simple method to avoid boilerplate.
     * <p>
     * If you need to preserve the logging configuration, and you only need to
     * add a single resource, you can use {@link #resourceExtensionFor(Object)}.
     *
     * @return a new {@link ResourceExtension.Builder} instance
     * @see #resourceExtensionFor(Object)
     * @see ResourceExtension.Builder#bootstrapLogging(boolean)
     */
    public static ResourceExtension.Builder resourceBuilderPreservingLogbackConfig() {
        return ResourceExtension.builder().bootstrapLogging(false);
    }
}
