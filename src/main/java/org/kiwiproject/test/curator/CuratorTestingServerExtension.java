package org.kiwiproject.test.curator;

import static com.google.common.base.Preconditions.checkArgument;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.retry.RetryOneTime;
import org.apache.curator.test.TestingServer;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.kiwiproject.net.LocalPortChecker;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * A JUnit Jupiter {@link org.junit.jupiter.api.extension.Extension Extension} that sets up a Curator {@link TestingServer}
 * for (integration) tests that require a ZooKeeper server to be running.
 * <p>
 * This extension starts a {@link TestingServer} before any tests run, and shuts it down after all tests have run.
 * It should be declared as {@code static} and registered using
 * {@link org.junit.jupiter.api.extension.RegisterExtension RegisterExtension}. Example:
 * <pre>
 * {@literal @}RegisterExtension
 * static final CuratorTestingServerExtension ZK_TEST_SERVER = new CuratorTestingServerExtension();
 * </pre>
 * <p>
 * One important thing to note is that because this uses {@link BeforeAllCallback}, any tests that use
 * {@link org.junit.jupiter.api.Nested Nested} will cause the {@link BeforeAllCallback#beforeAll(ExtensionContext)}
 * method to be executed again. This is why the client state check exists as the first thing in that method, and by
 * skipping initialization if the client is in the STARTED state, we can accommodate using nested classes. I am not
 * sure if this is the "correct" way to do this in Jupiter, but it works and doesn't seem like it will hurt anything.
 */
@Slf4j
public class CuratorTestingServerExtension implements BeforeAllCallback, AfterAllCallback {

    private static final LocalPortChecker PORT_CHECKER = new LocalPortChecker();

    private static final boolean START_SERVER = false;
    private static final int SESSION_TIMEOUT_MS = 5_000;
    private static final int CONNECTION_TIMEOUT_MS = 1_000;
    private static final int SLEEP_BETWEEN_RETRY_MS = 500;
    private static final int MAX_CONNECT_WAIT_TIME_SECONDS = 5;

    private final TestingServer testingServer;

    @Getter
    private final CuratorFramework client;

    public CuratorTestingServerExtension() {
        this(PORT_CHECKER.findFirstOpenPortAbove(1024).orElseThrow(IllegalStateException::new));
    }

    public CuratorTestingServerExtension(int port) {
        checkArgument(port >= 0 && port <= 0xFFFF, "Invalid port: %s", port);

        LOG.trace("Using {} as testing server port", port);

        try {
            testingServer = new TestingServer(port, START_SERVER);
        } catch (Exception e) {
            throw new CuratorTestingServerException("Error creating testing server on port " + port, e);
        }

        client = CuratorFrameworkFactory.newClient(
                testingServer.getConnectString(),
                SESSION_TIMEOUT_MS,
                CONNECTION_TIMEOUT_MS,
                new RetryOneTime(SLEEP_BETWEEN_RETRY_MS)
        );
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (client.getState() == CuratorFrameworkState.STARTED) {
            LOG.trace("Skip initialization since client is started. Maybe we are in a @Nested test class?");
            return;
        }

        LOG.trace("Starting TestingServer and CuratorFramework client");
        testingServer.start();
        client.start();
        client.blockUntilConnected(MAX_CONNECT_WAIT_TIME_SECONDS, TimeUnit.SECONDS);
        LOG.trace("Testing server with connect string {} started on port {} with temp directory {}; client is connected",
                testingServer.getConnectString(),
                testingServer.getPort(),
                testingServer.getTempDirectory());
    }

    @Override
    public void afterAll(ExtensionContext context) {
        try {
            LOG.trace("Closing client and test server");
            client.close();
            testingServer.close();
        } catch (Exception e) {
            throw new CuratorTestingServerException("Error stopping testing server on port " + getPort(), e);
        }
        LOG.trace("Client and test server closed");
    }

    /**
     * @return the connection string of the {@link TestingServer}
     */
    public String getConnectString() {
        return testingServer.getConnectString();
    }

    /**
     * @return the port of the {@link TestingServer}
     */
    public int getPort() {
        return testingServer.getPort();
    }

    /**
     * @return the temporary directory that the {@link TestingServer} is using
     */
    public File getTempDirectory() {
        return testingServer.getTempDirectory();
    }
}
