package org.kiwiproject.test.dropwizard.resource;

import io.dropwizard.testing.junit5.ResourceExtension;
import lombok.experimental.UtilityClass;

/**
 * Test utility for Dropwizard resource test classes using {@link ResourceExtension}.
 */
@UtilityClass
public class DropwizardResourceTests {

    /**
     * Create a {@link ResourceExtension} builder that preserves the existing Logback
     * configuration by disabling the logging boostrap.
     * <p>
     * Tired of writing {@code ResourceExtension.builder().bootstrapLogging(false)}
     * every time you create a {@link ResourceExtension}? Then use this absurdly
     * simple method to avoid boilerplate.
     *
     * @return a new {@link ResourceExtension.Builder} instance
     * @see ResourceExtension.Builder#bootstrapLogging(boolean)
     */
    public ResourceExtension.Builder resourceBuilderPreservingLogbackConfig() {
        return ResourceExtension.builder().bootstrapLogging(false);
    }
}
