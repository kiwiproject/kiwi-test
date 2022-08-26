package org.kiwiproject.test.junit.jupiter;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.UncheckedIOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

@DisplayName("H2FileBasedDatabaseExtension static utility tests")
class H2FileBasedDatabaseExtensionUtilitiesTest {

    @Nested
    class DeleteOrThrowUnchecked {

        // Make Sonar happy and cover the catch block
        @Test
        void shouldThrowUncheckedIOExeption_WhenPathIsNotValid() {
            var p = Path.of("/this/does/not/exist");
            assertThatThrownBy(() -> H2FileBasedDatabaseExtension.deleteOrThrowUnchecked(p))
                    .isExactlyInstanceOf(UncheckedIOException.class)
                    .hasCauseExactlyInstanceOf(NoSuchFileException.class);
        }
    }
}
