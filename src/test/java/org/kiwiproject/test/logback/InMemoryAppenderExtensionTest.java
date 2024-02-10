package org.kiwiproject.test.logback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kiwiproject.test.junit.jupiter.ClearBoxTest;

@DisplayName("InMemoryAppenderExtension")
@Slf4j
class InMemoryAppenderExtensionTest {

    @Test
    void shouldUseLogbackTestFileAsDefaultConfigLocation() {
        var extension = new InMemoryAppenderExtension(InMemoryAppenderExtensionTest.class);
        assertThat(extension.getLogbackConfigFilePath()).isNull();
    }

    @Test
    void shouldAcceptCustomConfigLocation() {
        var customLocation = "acme-test-logback.xml";
        var extension = new InMemoryAppenderExtension(InMemoryAppenderExtensionTest.class)
                .withLogbackConfigFilePath(customLocation);

        assertThat(extension.getLogbackConfigFilePath()).isEqualTo(customLocation);
    }

    @ClearBoxTest
    void shouldReturnNewLogbackTestHelperInstances() {
        var extension = new InMemoryAppenderExtension(InMemoryAppenderExtensionTest.class);

        var helper1 = extension.getLogbackTestHelper();
        var helper2 = extension.getLogbackTestHelper();
        var helper3 = extension.getLogbackTestHelper();

        assertThat(helper1).isNotSameAs(helper2);
        assertThat(helper2).isNotSameAs(helper3);
    }

    @ClearBoxTest
    void shouldGetAppenderWhenExists() {
        var logbackLogger = mock(Logger.class);
        var appender = new InMemoryAppender();
        when(logbackLogger.getAppender(anyString())).thenReturn(appender);

        var extension = new InMemoryAppenderExtension(InMemoryAppenderExtensionTest.class);

        var returnedAppender = extension.getAppender(logbackLogger);
        assertThat(returnedAppender).isSameAs(appender);

        verify(logbackLogger, only()).getAppender(expectedAppenderName());
    }

    private static String expectedAppenderName() {
        return InMemoryAppenderExtensionTest.class.getSimpleName() + "Appender";
    }

    @ClearBoxTest
    void shouldResetLogbackWhenAppenderDoesNotExist() {
        var logbackLogger = mock(Logger.class);
        var appender = new InMemoryAppender();

        // Simulate initially not getting the appender
        // then getting an appender (once Logback has been reset)
        when(logbackLogger.getAppender(anyString()))
                .thenReturn(null)
                .thenReturn(appender);

        var logbackTestHelper = mock(LogbackTestHelper.class);

        var appenderName = "MyAppender";
        var extension = new InMemoryAppenderExtension(InMemoryAppenderExtensionTest.class, appenderName) {
            @Override
            protected LogbackTestHelper getLogbackTestHelper() {
                return logbackTestHelper;
            }
        };

        var customConfig = "acme-logback-test.xml";
        extension.withLogbackConfigFilePath(customConfig);

        var returnedAppender = extension.getAppender(logbackLogger);
        assertThat(returnedAppender).isSameAs(appender);

        verify(logbackLogger, times(2)).getAppender(appenderName);
        verifyNoMoreInteractions(logbackLogger);

        verify(logbackTestHelper, only()).resetLogbackWithDefaultOrConfig(customConfig);
    }
}
