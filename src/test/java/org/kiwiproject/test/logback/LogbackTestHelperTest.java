package org.kiwiproject.test.logback;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
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

    @Test
    void shouldDelegateToLogbackTestHelpersWithFallback() {
        // Verify the delegation without actually resetting Logback
        // by passing in config files that don't exist.

        assertThatIllegalArgumentException()
                .isThrownBy(() ->
                        new LogbackTestHelper().resetLogback("acme-test-logback.xml", "acme-logback.xml"))
                .withMessage("Did not find any of the Logback configurations: [acme-test-logback.xml, acme-logback.xml]");
    }
}
