package org.kiwiproject.test.junit.jupiter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Optional;

@DisplayName("JupiterHelpers")
class JupiterHelpersTest {

    @Nested
    class IsTestClassNested {

        private ExtensionContext context;

        @BeforeEach
        void setUp() {
            context = mock(ExtensionContext.class);
        }

        @Test
        void shouldReturnFalse_WhenTestClassNotAnnotatedWithNested() {
            when(context.getTestClass()).thenReturn(Optional.of(JupiterHelpersTest.class));

            assertThat(JupiterHelpers.isTestClassNested(context)).isFalse();
        }

        @Test
        void shouldReturnTrue_WhenTestClassIsAnnotatedWithNested() {
            when(context.getTestClass()).thenReturn(Optional.of(IsTestClassNested.class));

            assertThat(JupiterHelpers.isTestClassNested(context)).isTrue();
        }

        @Test
        void shouldReturnTrue_WhenTestClassIsNotPresent_AndNestedClass_IsInUniqueId() {
            when(context.getTestClass()).thenReturn(Optional.empty());
            when(context.getUniqueId()).thenReturn(
                    "[engine:junit-jupiter]/[class:org.kiwiproject.test.junit.jupiter.SampleTest]/[nested-class:SampleNestedTest]");

            assertThat(JupiterHelpers.isTestClassNested(context)).isTrue();
        }

        @Test
        void shouldReturnFalse_WhenTestClassIsNotPresent_AndNestedClass_NotInUniqueId() {
            when(context.getTestClass()).thenReturn(Optional.empty());
            when(context.getUniqueId()).thenReturn(
                    "[engine:junit-jupiter]/[class:org.kiwiproject.test.junit.jupiter.SampleTest]");

            assertThat(JupiterHelpers.isTestClassNested(context)).isFalse();
        }
    }
}
