package org.kiwiproject.test.okhttp3.mockwebserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Strings;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.kiwiproject.test.junit.jupiter.params.provider.MinimalBlankStringSource;
import org.kiwiproject.util.function.KiwiConsumers;

import java.io.IOException;
import java.net.URI;
import java.util.function.Consumer;

@DisplayName("MockWebServerExtension")
class MockWebServerExtensionTest {

    @Nested
    class Constructors {

        @Test
        void shouldRequireNonNullServer() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new MockWebServerExtension(null, KiwiConsumers.noOp()))
                    .withMessage("server must not be null");
        }

        @SuppressWarnings("resource")
        @Test
        void shouldAllowNullServerCustomizer() {
            var server = new MockWebServer();
            assertThatCode(() -> new MockWebServerExtension(server, null))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldCreateServer() {
            var extension = new MockWebServerExtension();

            assertThat(extension.server()).isNotNull();
        }

        @Test
        void shouldSetSpecificServer() {
            var server = new MockWebServer();
            var extension = new MockWebServerExtension(server, null);

            assertThat(extension.server()).isSameAs(server);
        }
    }

    @Nested
    class Builder {

        @Test
        void shouldRequireNonNullServer() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> MockWebServerExtension.builder().build())
                    .withMessage("server must not be null");
        }

        @SuppressWarnings("resource")
        @Test
        void shouldAllowNullServerCustomizer() {
            var server = new MockWebServer();
            assertThatCode(() -> MockWebServerExtension.builder()
                    .server(server)
                    .serverCustomizer(null)
                    .build())
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldSetSpecificServer() {
            var server = new MockWebServer();
            var extension = MockWebServerExtension.builder()
                    .server(server)
                    .build();

            assertThat(extension.server()).isSameAs(server);
        }
    }

    @Nested
    class BeforeEachMethod {

        @Test
        void shouldStartServer() throws IOException {
            var server = mock(MockWebServer.class);
            when(server.url(anyString())).thenReturn(mock(HttpUrl.class));

            var extension = MockWebServerExtension.builder().server(server).build();
            extension.beforeEach(null);

            verify(server).start();
        }

        @Test
        void shouldCallCustomizer() throws IOException {
            var server = mock(MockWebServer.class);
            when(server.url(anyString())).thenReturn(mock(HttpUrl.class));

            Consumer<MockWebServer> customizer = mock();

            var extension = MockWebServerExtension.builder()
                    .server(server)
                    .serverCustomizer(customizer)
                    .build();
            extension.beforeEach(null);

            verify(customizer, only()).accept(server);
        }

        @Test
        void shouldAssignURI() throws IOException {
            var server = mock(MockWebServer.class);
            var httpUrl = mock(HttpUrl.class);
            var uri = URI.create("/path");
            when(httpUrl.uri()).thenReturn(uri);
            when(server.url(anyString())).thenReturn(httpUrl);

            var extension = MockWebServerExtension.builder().server(server).build();
            extension.beforeEach(null);

            assertThat(extension.uri()).isSameAs(uri);
        }
    }

    @Nested
    class AfterEachMethod {

        @Test
        void shouldCloseServer() throws IOException {
            var server = mock(MockWebServer.class);

            var extension = MockWebServerExtension.builder().server(server).build();
            extension.afterEach(null);

            verify(server, only()).close();
        }

        @Test
        void shouldIgnoreExceptionsThrownByServerClose() throws IOException {
            var server = mock(MockWebServer.class);
            doThrow(new IOException("i/o error closing server"))
                    .when(server)
                    .close();

            var extension = MockWebServerExtension.builder().server(server).build();

            assertThatCode(() -> extension.afterEach(null))
                    .doesNotThrowAnyException();

            verify(server, only()).close();
        }
    }

    @Nested
    class UriMethod {

        @Test
        void shouldThrowIllegalStateException_WhenCalledBeforeServerIsStarted() {
            var extension = new MockWebServerExtension();

            assertThatIllegalStateException()
                    .isThrownBy(extension::uri)
                    .withMessage("server has not been started; only call this after beforeEach executes");
        }
    }

    @Nested
    class UriWithPath {

        @Test
        void shouldThrowIllegalStateException_WhenCalledBeforeServerIsStarted() {
            var extension = new MockWebServerExtension();

            assertThatIllegalStateException()
                    .isThrownBy(() -> extension.uri("/path"))
                    .withMessage("server has not been started; only call this after beforeEach executes");
        }

        @Test
        void shouldNotAllowNullPath() throws IOException {
            var server = new MockWebServer();
            var extension = MockWebServerExtension.builder()
                    .server(server)
                    .build();

            extension.beforeEach(null);

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> extension.uri(null))
                    .withMessage("path must not be null");
        }

        @ParameterizedTest
        @MinimalBlankStringSource
        void shouldNormalizeBlankPaths(String path) throws IOException {
            var server = new MockWebServer();
            var extension = MockWebServerExtension.builder()
                    .server(server)
                    .build();

            extension.beforeEach(null);

            // MinimalBlankStringSource gives us a null, which we need to ignore,
            // so convert it to an empty string.
            var nonNullPath = Strings.nullToEmpty(path);

            assertThat(extension.uri(nonNullPath))
                    .extracting(URI::getPath)
                    .isEqualTo("/");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "",
                "/",
                "/status",
                "/foo/bar"
        })
        void shouldResolvePaths(String path) throws IOException {
            var server = new MockWebServer();
            var extension = MockWebServerExtension.builder()
                    .server(server)
                    .build();

            extension.beforeEach(null);

            // handle the special case of an empty string; the resulting
            // path should end with a slash
            var expectedPath = path.isEmpty() ? "/" : path;

            assertThat(extension.uri(path))
                    .isEqualTo(extension.uri().resolve(path))
                    .extracting(URI::getPath)
                    .isEqualTo(expectedPath);
        }
    }
}
