package org.kiwiproject.test.curator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.retry.RetryOneTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.TimeUnit;

/**
 * This tests the case when a test has requested the Curator {@link org.apache.curator.test.TestingServer} to start
 * immediately, instead of waiting for the @{@link org.junit.jupiter.api.extension.BeforeAllCallback} during
 * the normal JUnit Jupiter lifecycle. To do this, it uses a custom Jupiter extension which attempts to connect to
 * the testing ZooKeeper server. This test also contains a nested test class to ensure everything works regardless
 * of the test structure.
 */
@DisplayName("CuratorTestingServerExtension with immediate start")
@Slf4j
class CuratorTestingServerExtensionImmediateStartTest {

    @RegisterExtension
    static final CuratorTestingServerExtension ZK_TEST_SERVER = CuratorTestingServerExtension.newStartingImmediately();

    /**
     * Simulates another extension that requires a ZooKeeper server. Since {@link org.apache.curator.test.TestingServer}
     * doesn't expose any way to check its state (i.e. is it started or not), this gets the {@link CuratorFramework}
     * client from the {@link CuratorTestingServerExtension} and attempts to start it, blocking until connected (with
     * a timeout to ensure it stops if it cannot connect). This is an indirect way of determining whether the testing
     * server has started.
     */
    static class ZooKeeperUsingExtension implements BeforeAllCallback {

        @Getter
        private boolean testingServerStarted;

        /**
         * Creates a new Curator client and attempts to connect to the testing server.
         */
        @Override
        public void beforeAll(ExtensionContext extensionContext) throws Exception {
            var sessionTimeoutMs = 2_000;
            var connectionTimeoutMs = 1_000;
            var sleepMsBetweenRetry = 500;
            var retryPolicy = new RetryOneTime(sleepMsBetweenRetry);

            try (var client = CuratorFrameworkFactory.newClient(ZK_TEST_SERVER.getConnectString(),
                    sessionTimeoutMs,
                    connectionTimeoutMs,
                    retryPolicy)) {
                client.start();
                testingServerStarted = client.blockUntilConnected(1, TimeUnit.SECONDS);
            }
        }
    }

    @RegisterExtension
    static final ZooKeeperUsingExtension ZOOKEEPER_USING_EXTENSION = new ZooKeeperUsingExtension();

    private CuratorFramework client;

    @BeforeEach
    void setUp(TestInfo testInfo) {
        LOG.trace("Test: {}", testInfo.getDisplayName());

        client = ZK_TEST_SERVER.getClient();
    }

    @Test
    void shouldHaveStartedTestingServerImmediately() {
        assertThat(ZOOKEEPER_USING_EXTENSION.isTestingServerStarted())
                .describedAs("The TestingServer should have started immediately (when constructed) but did not")
                .isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {-42, -1, 65_536, 65_537, 90_000})
    void shouldRejectInvalidPorts(int value) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CuratorTestingServerExtension(value))
                .withMessage("Invalid port: " + value);
    }

    @Test
    void shouldHavePort() {
        assertThat(ZK_TEST_SERVER.getPort()).isPositive();
    }

    @Test
    void shouldHaveClient() {
        assertThat(client.getState()).isEqualTo(CuratorFrameworkState.STARTED);
    }

    @Nested
    class CanUseClient {

        @Test
        void toCheckExistenceOfPath() throws Exception {
            var stat = client.checkExists().forPath("/foo");
            assertThat(stat).isNull();
        }

        @Test
        void toCreateZnode() throws Exception {
            var path = "/bar/baz";
            var createdPath = client.create().creatingParentContainersIfNeeded().forPath(path);
            assertThat(createdPath).isEqualTo(path);

            var stat = client.checkExists().forPath(createdPath);
            assertThat(stat).isNotNull();
        }
    }
}
