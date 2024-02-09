package org.kiwiproject.test.logback;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kiwiproject.test.junit.jupiter.ResetLogbackLoggingExtension;

import ch.qos.logback.core.joran.spi.JoranException;

/**
 * Unit test for {@link LogbackTestHelpers}. This mainly tests invalid arguments
 * and invalid Logback configuration. {@link LogbackTestHelpersIntegrationTest}
 * performs a full integration test of the reset behavior.
 */
@DisplayName("LogbackTestHelpers")
@ExtendWith(ResetLogbackLoggingExtension.class)
public class LogbackTestHelpersTest {

    @Test
    void shouldThrowIllegalArgument_WhenInvalidLogbackConfigPath() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> LogbackTestHelpers.resetLogback("dne.xml"));
    }

    @Test
    void shouldThrowUncheckedJoranException_WhenInvalidLogbackConfig() {
        assertThatExceptionOfType(UncheckedJoranException.class)
                .isThrownBy(() -> LogbackTestHelpers.resetLogback("LogbackTestHelpersTest/invalid-logback-test.xml"))
                .withCauseExactlyInstanceOf(JoranException.class);
    }
}
