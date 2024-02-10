package org.kiwiproject.test.junit.jupiter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
