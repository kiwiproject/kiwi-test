package org.kiwiproject.test.okhttp3.mockwebserver;

import static org.assertj.core.api.Assertions.assertThat;

import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.kiwiproject.io.KiwiIO;

import java.io.IOException;
import java.net.URI;

@DisplayName("MockWebServers")
class MockWebServersTest {

    private MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() {
        KiwiIO.closeQuietly(server);
    }

    @Nested
    class UriMethod {

        @ParameterizedTest
        @ValueSource(strings = { "", "/", "/status", "/status/", "/users/active" })
        void shouldCreateUri_WithPath(String path) {
            var url = server.url(path).toString();
            var expectedUri = URI.create(url);
            assertThat(MockWebServers.uri(server, path)).isEqualTo(expectedUri);
        }
    }
}
