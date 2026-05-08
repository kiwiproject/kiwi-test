package org.kiwiproject.test.junit.jupiter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.kiwiproject.test.logback.LogbackTestHelper;

@DisplayName("ResetLogbackLoggingExtension")
class ResetLogbackLoggingExtensionTest {

    @Test
    void shouldConstructWithNullAsDefaultConfigLocation() {
        var extension = new ResetLogbackLoggingExtension();
        assertThat(extension.getLogbackConfigFilePath()).isNull();
    }

    @Test
    void shouldBuildWithNullAsDefaultConfigLocation() {
        var extension = ResetLogbackLoggingExtension.builder().build();
        assertThat(extension.getLogbackConfigFilePath()).isNull();
    }

    @Test
    void shouldAllowCustomConfigLocation() {
        var customLocation = "acme-test-logback.xml";

        var extension = ResetLogbackLoggingExtension.builder()
                .logbackConfigFilePath(customLocation)
                .build();

        assertThat(extension.getLogbackConfigFilePath()).isEqualTo(customLocation);
    }

    @Nested
    class BeforeAll {

        private LogbackTestHelper helper;
        private ResetLogbackLoggingExtension extension;
        private ExtensionContext extensionContext;

        @BeforeEach
        void setUp() {
            helper = mock(LogbackTestHelper.class);
            extensionContext = mock(ExtensionContext.class);
        }

        @Test
        void shouldResetLogbackWithNullConfigByDefault() {
            extension = spy(new ResetLogbackLoggingExtension());
            when(extension.getLogbackTestHelper()).thenReturn(helper);

            extension.beforeAll(extensionContext);

            verify(helper).resetLogbackWithDefaultOrConfig(isNull());
        }

        @Test
        void shouldResetLogbackWithCustomConfig() {
            var customConfig = "acme-test-logback.xml";
            extension = spy(ResetLogbackLoggingExtension.builder()
                    .logbackConfigFilePath(customConfig)
                    .build());
            when(extension.getLogbackTestHelper()).thenReturn(helper);

            extension.beforeAll(extensionContext);

            verify(helper).resetLogbackWithDefaultOrConfig(eq(customConfig));
        }
    }

    @Nested
    class AfterAll {

        private LogbackTestHelper helper;
        private ResetLogbackLoggingExtension extension;
        private ExtensionContext extensionContext;

        @BeforeEach
        void setUp() {
            helper = mock(LogbackTestHelper.class);
            extensionContext = mock(ExtensionContext.class);
        }

        @Test
        void shouldResetLogbackWithNullConfigByDefault() {
            extension = spy(new ResetLogbackLoggingExtension());
            when(extension.getLogbackTestHelper()).thenReturn(helper);

            extension.afterAll(extensionContext);

            verify(helper).resetLogbackWithDefaultOrConfig(isNull());
        }

        @Test
        void shouldResetLogbackWithCustomConfig() {
            var customConfig = "acme-test-logback.xml";
            extension = spy(ResetLogbackLoggingExtension.builder()
                    .logbackConfigFilePath(customConfig)
                    .build());
            when(extension.getLogbackTestHelper()).thenReturn(helper);

            extension.afterAll(extensionContext);

            verify(helper).resetLogbackWithDefaultOrConfig(eq(customConfig));
        }
    }
}
