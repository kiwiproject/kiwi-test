package org.kiwiproject.test.okhttp3.mockwebserver;

import lombok.experimental.UtilityClass;
import okhttp3.mockwebserver.MockWebServer;

import java.net.URI;

@UtilityClass
public class MockWebServers {

    public static URI uri(MockWebServer server, String path) {
        return URI.create(server.url(path).toString());
    }
}
