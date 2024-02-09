package org.kiwiproject.test.logback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assertions.assertAll;

import ch.qos.logback.core.joran.spi.JoranException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UncheckedJoranException")
class UncheckedJoranExceptionTest {

    @SuppressWarnings("ThrowableNotThrown")
    @Test
    void shouldRequireCause() {
        assertAll(
            () -> assertThatIllegalArgumentException().isThrownBy(() -> new UncheckedJoranException("oops", null)),
            () -> assertThatIllegalArgumentException().isThrownBy(() -> new UncheckedJoranException(null))
        );
    }

    @Test
    void shouldSetCause() {
        var joranException = new JoranException("Invalid XML");

        assertAll(
            () -> assertThat(new UncheckedJoranException("oops", joranException)).hasCause(joranException),
            () -> assertThat(new UncheckedJoranException(joranException)).hasCause(joranException)
        );
    }
}
