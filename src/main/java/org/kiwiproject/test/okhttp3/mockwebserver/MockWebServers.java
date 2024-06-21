package org.kiwiproject.test.okhttp3.mockwebserver;

import lombok.experimental.UtilityClass;

import java.net.URI;

import okhttp3.mockwebserver.MockWebServer;

@UtilityClass
public class MockWebServers {

    public static URI uri(MockWebServer server, String path) {
        return URI.create(server.url(path).toString());
    }
}
