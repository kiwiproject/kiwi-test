package org.kiwiproject.test.okhttp3.mockwebserver3;

import lombok.experimental.UtilityClass;
import mockwebserver3.MockWebServer;

import java.net.URI;

/**
 * Static test utilities related to {@link MockWebServer}.
 * <p>
 * Note that mockwebserver3 (com.squareup.okhttp3:mockwebserver3) and OkHttp
 * dependencies must be available at runtime. OkHttp is a transitive dependency
 * of mockwebserver3, so you should only need to add mockwebserver3.
 */
@UtilityClass
public class MockWebServers {

    /**
     * Get the "base" {@link URI} of the {@link MockWebServer} to which HTTP
     * requests can be made. It can also be used to {@link URI#resolve(URI) resolve}
     * other URIs relative to the base URI.
     *
     * @param server the {@link MockWebServer}
     * @return a {@link URI}
     */
    public static URI uri(MockWebServer server) {
        return uri(server, "");
    }

    /**
     * Create a {@link URI} which can be used to make HTTP (or HTTPS) requests
     * to the given {@link MockWebServer}.
     * <p>
     * Appends the given path to the server base URL.
     *
     * @param server the {@link MockWebServer}
     * @param path   the path relative to the server's base URI
     * @return a {@link URI} to connect to the server, with the given path
     */
    public static URI uri(MockWebServer server, String path) {
        return server.url(path).uri();
    }
}
