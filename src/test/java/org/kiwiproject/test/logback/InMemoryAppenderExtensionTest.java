package org.kiwiproject.test.logback;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
