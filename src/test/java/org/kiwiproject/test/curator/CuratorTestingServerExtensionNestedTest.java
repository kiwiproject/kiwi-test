package org.kiwiproject.test.curator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * This test is intentionally structured with multiple @{@link Nested} test classes to verify that the
 * extension functions properly when there are top-level tests as well as tests within nested test classes. The
 * {@link CuratorTestingServerExtensionTest} has the same tests as this one does, except they are all top-level
 * tests with no nested test classes.
 */
@Slf4j
@DisplayName("CuratorTestingServerExtension with Nested test classes")
class CuratorTestingServerExtensionNestedTest {

    @RegisterExtension
    static final CuratorTestingServerExtension ZK_TEST_SERVER = new CuratorTestingServerExtension();

    private CuratorFramework client;

    @BeforeEach
    void setUp(TestInfo testInfo) {
        LOG.trace("Test: {}", testInfo.getDisplayName());

        client = ZK_TEST_SERVER.getClient();
    }

    /**
     * Top-level test.
     */
    @Test
    void shouldHavePort() {
        assertThat(ZK_TEST_SERVER.getPort()).isPositive();
    }

    /**
     * Top-level test.
     */
    @Test
    void shouldHaveConnectionString() {
        var port = ZK_TEST_SERVER.getPort();
        assertThat(ZK_TEST_SERVER.getConnectString()).isIn(
                "localhost:" + port,
                "127.0.0.1:" + port
        );
    }

    @Nested
    class Validation {

        @ParameterizedTest
        @ValueSource(ints = {-42, -1, 65_536, 65_537, 90_000})
        void shouldRejectInvalidPorts(int value) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new CuratorTestingServerExtension(value))
                    .withMessage("Invalid port: " + value);
        }
    }

    @Nested
    class Properties {

        @Test
        void shouldHavePort() {
            assertThat(ZK_TEST_SERVER.getPort()).isPositive();
        }

        @Test
        void shouldHaveConnectionString() {
            var port = ZK_TEST_SERVER.getPort();
            assertThat(ZK_TEST_SERVER.getConnectString()).isIn(
                    "localhost:" + port,
                    "127.0.0.1:" + port
            );
        }

        @Test
        void shouldHaveTempDirectory() {
            assertThat(ZK_TEST_SERVER.getTempDirectory())
                    .isDirectory()
                    .canRead()
                    .canWrite();
        }

        @Test
        void shouldHaveClient() {
            assertThat(client.getState()).isEqualTo(CuratorFrameworkState.STARTED);
        }
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
