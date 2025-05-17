package org.kiwiproject.test.okhttp3.mockwebserver;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.kiwiproject.base.KiwiPreconditions.checkArgumentNotNull;
import static org.kiwiproject.base.KiwiPreconditions.requireNotNull;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import okhttp3.mockwebserver.MockWebServer;
import org.jspecify.annotations.Nullable;
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
 * <p>
 * You can create an instance using the constructors or builder.
 * <p>
 * If you need to perform customization of the {@code MockWebServer}, you can
 * provide a "customizer" as a {@link Consumer} that accepts a {@code MockWebServer}.
 * This allows you to configure the server to use TLS, to specify which protocols
 * are supported, etc.
 */
public class MockWebServerExtension implements BeforeEachCallback, AfterEachCallback {

    /**
     * The {@link MockWebServer} started by this extension.
     */
    @Getter
    @Accessors(fluent = true)
    private final MockWebServer server;

    private final Consumer<MockWebServer> serverCustomizer;

    // Assigned after the server is started
    private URI uri;

    /**
     * Create a new instance.
     * <p>
     * The extension will use a default {@link MockWebServer} instance.
     * <p>
     * If you want to customize the server or provide your own {@link MockWebServer},
     * use the builder instead of this constructor.
     */
    public MockWebServerExtension() {
        this(KiwiConsumers.noOp());
    }

    /**
     * Create a new instance that will create a new {@link MockWebServer} and
     * customize it using the {@code serverCustomizer}.
     *
     * @param serverCustomizer allows a test to configure the server, e.g., to customize the protocols
     *                         it supports or to serve requests via HTTPS over TLS. Must not be {@code null}.
     */
    public MockWebServerExtension(Consumer<MockWebServer> serverCustomizer) {
        this(new MockWebServer(), requireNotNull(serverCustomizer, "serverCustomizer must not be null"));
    }

    /**
     * Create a new instance that will use the given {@link MockWebServer} and customizer.
     *
     * @param server           the server
     * @param serverCustomizer allows a test to configure the server, e.g., to customize the protocols
     *                         it supports or to serve requests via HTTPS over TLS. If this is
     *                         {@code null}, it is ignored.
     */
    @Builder
    MockWebServerExtension(MockWebServer server, @Nullable Consumer<MockWebServer> serverCustomizer) {
        this.server = requireNotNull(server, "server must not be null");
        this.serverCustomizer = isNull(serverCustomizer) ? KiwiConsumers.noOp() : serverCustomizer;
    }

    /**
     * Calls the server customizer {@link Consumer} if present, then starts the server
     * and assigns the base {@link #uri()}.
     *
     * @param context the current extension context; never {@code null}
     * @throws IOException if an error occurs starting the {@link MockWebServer}
     */
    @Override
    public void beforeEach(ExtensionContext context) throws IOException {
        serverCustomizer.accept(server);
        server.start();
        uri = MockWebServers.uri(server);
    }

    /**
     * Closes the {@link MockWebServer}, ignoring any exceptions that are thrown.
     *
     * @param context the current extension context; never {@code null}
     */
    @Override
    public void afterEach(ExtensionContext context) {
        KiwiIO.closeQuietly(server);
    }

    /**
     * The base {@link URI} of the {@link MockWebServer}.
     * <p>
     * This is available after the {@link MockWebServer} has been started.
     *
     * @throws IllegalStateException if called before the server is started
     */
    public URI uri() {
        checkState(nonNull(uri),
                "server has not been started; only call this after beforeEach executes");
        return uri;
    }

    /**
     * Get a {@link URI} with the given {@code path} that can be used to
     * make calls to the {@link MockWebServer}.
     * <p>
     * This can be called after the {@link MockWebServer} has been started.
     *
     * @param path the path, not null. If the path is blank (whitespace only),
     *             then it is normalized to an empty string before resolving
     *             the relative URI.
     * @return a new {@link URI}
     * @throws IllegalStateException if called before the server is started
     */
    public URI uri(String path) {
        checkArgumentNotNull(path, "path must not be null");
        var normalizedPath = defaultIfBlank(path, "");
        return uri().resolve(normalizedPath);
    }
}
