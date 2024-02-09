package org.kiwiproject.test.junit.jupiter;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.ClassicConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ResetLogbackLoggingExtension")
class ResetLogbackLoggingExtensionTest {

    @Test
    void shouldConstructWithLogbackTestFileAsDefaultConfigLocation() {
        var extension = new ResetLogbackLoggingExtension();
        assertThat(extension.getLogbackConfigFilePath())
                .isEqualTo(ClassicConstants.TEST_AUTOCONFIG_FILE);
    }

    @Test
    void shouldBuildWithLogbackTestFileAsDefaultConfigLocation() {
        var extension = ResetLogbackLoggingExtension.builder().build();
        assertThat(extension.getLogbackConfigFilePath())
                .isEqualTo(ClassicConstants.TEST_AUTOCONFIG_FILE);
    }

    @Test
    void shouldAllowCustomConfigLocation() {
        var customLocation = "acme-test-logback.xml";

        var extension = ResetLogbackLoggingExtension.builder()
                .logbackConfigFilePath(customLocation)
                .build();

        assertThat(extension.getLogbackConfigFilePath()).isEqualTo(customLocation);
    }
}
