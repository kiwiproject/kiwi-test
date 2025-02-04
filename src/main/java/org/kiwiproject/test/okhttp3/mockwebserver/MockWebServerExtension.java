package org.kiwiproject.test.okhttp3.mockwebserver;

import static java.util.Objects.isNull;
import static org.kiwiproject.base.KiwiPreconditions.requireNotNull;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.kiwiproject.io.KiwiIO;
import org.kiwiproject.util.function.KiwiConsumers;

import java.io.IOException;
import java.net.URI;
import java.util.function.Consumer;

/**
 * A simple JUnit Jupiter extension that creates and starts a {@link MockWebServer}
 * before <em>each</em> test, and shuts it down after <em>each</em> test.
 */
public class MockWebServerExtension implements BeforeEachCallback, AfterEachCallback {

    /**
     * The {@link MockWebServer} started by this extension.
     */
    @Getter
    @Accessors(fluent = true)
    private final MockWebServer server;

    /**
     * The base {@link URI} of the {@link MockWebServer}.
     * <p>
     * This is available after the {@link MockWebServer} has been started.
     */
    @Getter
    @Accessors(fluent = true)
    private URI uri;

    private final Consumer<MockWebServer> serverCustomizer;

    /**
     * Create a new instance.
     * <p>
     * The extension will use a default {@link MockWebServer} instance.
     * <p>
     * If you want to customize the server or provide your own {@link MockWebServer},
     * use the builder instead of this constructor.
     */
    public MockWebServerExtension() {
        this(new MockWebServer(), KiwiConsumers.noOp());
    }

    /**
     * Create a new instance that will use the given {@link MockWebServer} and customizer.
     *
     * @param server           the server
     * @param serverCustomizer allows a test to configure the server, e.g., to customize the protocols
     *                         it supports or to serve requests via HTTPS over TLS.
     */
    @Builder
    MockWebServerExtension(MockWebServer server, Consumer<MockWebServer> serverCustomizer) {
        this.server = requireNotNull(server, "server must not be null");
        this.serverCustomizer = isNull(serverCustomizer) ? KiwiConsumers.noOp() : serverCustomizer;
    }

    @Override
    public void beforeEach(ExtensionContext context) throws IOException {
        serverCustomizer.accept(server);
        server.start();
        uri = MockWebServers.uri(server);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        KiwiIO.closeQuietly(server);
    }

    /**
     * Get a {@link URI} with the give {@code path} that can be used to
     * make calls to the {@link MockWebServer}.
     * <p>
     * This can be called after the {@link MockWebServer} has been started.
     *
     * @param path the path
     * @return a new {@link URI}
     */
    public URI uri(String path) {
        return uri.resolve(path);
    }
}
