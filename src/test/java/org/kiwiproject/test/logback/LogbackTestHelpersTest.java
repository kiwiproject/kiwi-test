package org.kiwiproject.test.logback;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import ch.qos.logback.core.joran.spi.JoranException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.kiwiproject.test.junit.jupiter.ResetLogbackLoggingExtension;
import org.kiwiproject.test.junit.jupiter.params.provider.MinimalBlankStringSource;

/**
 * Unit test for {@link LogbackTestHelpers}. This mainly tests invalid arguments
 * and invalid Logback configuration. {@link LogbackTestHelpersIntegrationTest}
 * performs a full integration test of the reset behavior.
 */
@DisplayName("LogbackTestHelpers")
@ExtendWith(ResetLogbackLoggingExtension.class)
class LogbackTestHelpersTest {

    @Nested
    class ThrowsIllegalArgumentException {

        @Test
        void whenInvalidLogbackConfigPath() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> LogbackTestHelpers.resetLogback("dne.xml"))
                    .withMessage("Did not find any of the Logback configurations: [dne.xml]");
        }

        @Test
        void whenInvalidLogbackConfigPaths() {
            assertThatIllegalArgumentException().isThrownBy(() ->
                            LogbackTestHelpers.resetLogback("dne1.xml", "dne2.xml", "dne3.xml"))
                    .withMessage("Did not find any of the Logback configurations: [dne1.xml, dne2.xml, dne3.xml]");
        }

        @ParameterizedTest
        @MinimalBlankStringSource
        void whenGivenBlankConfigLocation(String location) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> LogbackTestHelpers.resetLogback(location))
                    .withMessage("logbackConfigFile must not be blank");
        }

        @Test
        void whenExplicitNullFallbackLocationsIsGiven() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> LogbackTestHelpers.resetLogback("acme-logback.xml", (String[]) null))
                    .withMessage("fallback locations vararg parameter must not be null");
        }

        @ParameterizedTest
        @MinimalBlankStringSource
        void whenFallbackLocationIsBlank(String fallbackLocation) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> LogbackTestHelpers.resetLogback("acme-test-logback.xml", fallbackLocation))
                    .withMessage("fallbackConfigFiles must not contain blank locations");
        }
    }

    @Test
    void shouldThrowUncheckedJoranException_WhenInvalidLogbackConfig() {
        assertThatExceptionOfType(UncheckedJoranException.class)
                .isThrownBy(() ->
                        LogbackTestHelpers.resetLogback("LogbackTestHelpersTest/invalid-logback-test.xml"))
                .withCauseExactlyInstanceOf(JoranException.class);
    }
}
