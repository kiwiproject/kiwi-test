package org.kiwiproject.test.dropwizard.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.kiwiproject.test.assertj.KiwiAssertJ.assertIsExactType;

import io.dropwizard.testing.junit5.ResourceExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.kiwiproject.test.junit.jupiter.ClearBoxTest;

@DisplayName("DropwizardResource")
class DropwizardResourceTestsTest {

    @Nested
    class ResourceBuilderPreservingLogbackConfig {

        private ResourceExtension.Builder builder;

        @BeforeEach
        void setUp() {
            builder = DropwizardResourceTests.resourceBuilderPreservingLogbackConfig();
        }

        @Test
        void shouldCreateResourceExtensionBuilder() {
            assertThat(builder).isNotNull();
        }

        @ClearBoxTest("this directly accesses a private field and assumes a specific class structure")
        void shouldSet_bootstrapLogging_toFalse() throws NoSuchFieldException, IllegalAccessException {
            var field = builder.getClass().getSuperclass().getDeclaredField("bootstrapLogging");
            assertThat(field)
                    .describedAs("We did not find the 'bootstrapLogging' field. Dropwizard might have moved it.")
                    .isNotNull();

            field.setAccessible(true);

            var value = field.get(builder);
            var booleanValue = assertIsExactType(value, Boolean.class);
            assertThat(booleanValue)
                    .describedAs("bootstrapLogging should be false")
                    .isFalse();
        }
    }

}
