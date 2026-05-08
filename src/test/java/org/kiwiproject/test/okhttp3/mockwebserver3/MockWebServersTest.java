package org.kiwiproject.test.okhttp3.mockwebserver3;

import static org.assertj.core.api.Assertions.assertThat;

import mockwebserver3.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URI;

@DisplayName("MockWebServers (mockwebserver3)")
class MockWebServersTest {

    private MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @Nested
    class UriMethod {

        @Test
        void shouldGetBaseUriOfServer() {
            var url = server.url("").toString();
            var expectedUri = URI.create(url);
            assertThat(MockWebServers.uri(server)).isEqualTo(expectedUri);
        }
    }

    @Nested
    class UriMethodWithPath {

        @ParameterizedTest
        @ValueSource(strings = { "", "/", "/status", "/status/", "/users/active" })
        void shouldCreateUri_WithPath(String path) {
            var url = server.url(path).toString();
            var expectedUri = URI.create(url);
            assertThat(MockWebServers.uri(server, path)).isEqualTo(expectedUri);
        }
    }
}
