package org.kiwiproject.test.logback;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.kiwiproject.test.junit.jupiter.params.provider.MinimalBlankStringSource;

@DisplayName("Logback")
class LogbackTestHelperTest {

    private LogbackTestHelper helper;

    @BeforeEach
    void setUp() {
        helper = spy(new LogbackTestHelper());

        doNothing().when(helper).resetLogback();
        doNothing().when(helper).resetLogback(anyString());
    }

    @Test
    void shouldResetLogbackWithCustomConfiguration() {
        var customConfig = "test-acme-logback.xml";
        helper.resetLogbackWithDefaultOrConfig(customConfig);

        verify(helper).resetLogback(customConfig);
    }

    @ParameterizedTest
    @MinimalBlankStringSource
    void shouldUseDefaultWhenCustomConfigurationIsBlank(String configFile) {
        helper.resetLogbackWithDefaultOrConfig(configFile);

        verify(helper).resetLogback();
    }
}
