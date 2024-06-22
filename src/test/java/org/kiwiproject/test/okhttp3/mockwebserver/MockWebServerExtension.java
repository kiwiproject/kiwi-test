package org.kiwiproject.test.okhttp3.mockwebserver;

import lombok.Getter;
import lombok.experimental.Accessors;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.kiwiproject.io.KiwiIO;

import java.io.IOException;

public class MockWebServerExtension implements BeforeEachCallback, AfterEachCallback {

    @Getter
    @Accessors(fluent = true)
    private MockWebServer server;

    @Override
    public void beforeEach(ExtensionContext context) throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        KiwiIO.closeQuietly(server);
    }
}
