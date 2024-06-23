package org.kiwiproject.test.okhttp3.mockwebserver;

import lombok.experimental.UtilityClass;
import okhttp3.mockwebserver.MockWebServer;

import java.net.URI;

/**
 * Static test utilities related to {@link MockWebServer}.
 * <p>
 * Note that MockWebServer (com.squareup.okhttp3:mockwebserver) and OkHttp
 * dependencies must be available at runtime. OkHttp is a transitive dependency
 * of mockwebserver, so you should only need to add mockwebserver.
 */
@UtilityClass
public class MockWebServers {

    public static URI uri(MockWebServer server, String path) {
        return URI.create(server.url(path).toString());
    }
}
