package org.kiwiproject.test.liquibase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assertions.assertAll;

import liquibase.exception.LiquibaseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UncheckedLiquibaseException")
class UncheckedLiquibaseExceptionTest {

    @SuppressWarnings("ThrowableNotThrown")
    @Test
    void shouldRequireCause() {
        assertAll(
                () -> assertThatIllegalArgumentException()
                        .isThrownBy(() -> new UncheckedLiquibaseException("oops", null)),

                () -> assertThatIllegalArgumentException()
                        .isThrownBy(() -> new UncheckedLiquibaseException(null))
        );
    }

    @Test
    void shouldSetCause() {
        var liquibaseException = new LiquibaseException("Invalid XML");

        assertAll(
                () -> assertThat(new UncheckedLiquibaseException("oops", liquibaseException))
                        .hasMessage("oops")
                        .hasCause(liquibaseException),

                () -> assertThat(new UncheckedLiquibaseException(liquibaseException))
                        .hasMessageContaining(liquibaseException.getMessage())
                        .hasCause(liquibaseException)
        );
    }
}
