package org.kiwiproject.test.mongo;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kiwiproject.net.KiwiInternetAddresses.SimpleHostInfo;
import org.kiwiproject.test.util.Fixtures;

@DisplayName("MongoTests")
@ExtendWith(SoftAssertionsExtension.class)
class MongoTestsTest {

    private static final String MONGO_HOST = "dev-mongo-1.dev.acme.com";
    private static final String SERVICE_NAME = "test-service";

    @Nested
    class NewMongoTestPropertiesWithServiceName {

        @Test
        void shouldBuildWithDefaultPort(SoftAssertions softly) {
            var properties = MongoTests.newMongoTestPropertiesWithServiceName(MONGO_HOST, SERVICE_NAME);

            assertProperties(softly, properties, MongoTests.DEFAULT_MONGO_PORT);
        }

        @Test
        void shouldBuildWithSpecifiedPort(SoftAssertions softly) {
            var mongoPort = 30_042;
            var properties = MongoTests.newMongoTestPropertiesWithServiceName(MONGO_HOST, mongoPort, SERVICE_NAME);

            assertProperties(softly, properties, mongoPort);
        }
    }

    @Nested
    class NewMongoTestPropertiesFromPom {

        @Test
        void shouldBuildWithDefaultPort(SoftAssertions softly) {
            var root = Fixtures.fixturePath("MongoTestsTest").toString();
            var properties = MongoTests.newMongoTestPropertiesFromPom(MONGO_HOST, root);

            assertProperties(softly, properties, "kiwi-test-mongo-service", MongoTests.DEFAULT_MONGO_PORT);
        }

        // Using the variant that uses the default root path, which will use kiwi-tests's POM file. Since kiwi-test
        // is a library, not a service or emulator as we define it, the calls should throw exceptions.
        @Test
        void shouldThrow_WhenPomIsNotAServiceOrEmulator(SoftAssertions softly) {
            softly.assertThatThrownBy(() -> MongoTests.newMongoTestPropertiesFromPom(MONGO_HOST))
                    .isExactlyInstanceOf(IllegalStateException.class);

            softly.assertThatThrownBy(() -> MongoTests.newMongoTestPropertiesFromPom(MONGO_HOST, MongoTests.DEFAULT_MONGO_PORT))
                    .isExactlyInstanceOf(IllegalStateException.class);

        }
    }

    private void assertProperties(SoftAssertions softly, MongoTestProperties properties, int mongoPort) {
        assertProperties(softly, properties, SERVICE_NAME, mongoPort);
    }

    private void assertProperties(SoftAssertions softly, MongoTestProperties properties, String serviceName, int mongoPort) {
        softly.assertThat(properties.getHostName()).isEqualTo(MONGO_HOST);
        softly.assertThat(properties.getPort()).isEqualTo(mongoPort);
        softly.assertThat(properties.getServiceName()).isEqualTo(serviceName);
        softly.assertThat(properties.getServiceHost()).isNotBlank();
        softly.assertThat(properties.getDatabaseName()).startsWith(serviceName);
        softly.assertThat(properties.getUri())
                .startsWith("mongodb://" + MONGO_HOST + ":" + mongoPort + "/")
                .endsWith(properties.getDatabaseName());
    }

    @Nested
    class FallbackSimpleHostInfo {

        @Test
        void shouldProvideLoopbackAddress() {
            var fallbackHostInfo = MongoTests.fallbackSimpleHostInfo();

            assertThat(fallbackHostInfo.getHostName()).isEqualTo("localhost");
            assertThat(fallbackHostInfo.getIpAddr()).isEqualTo("127.0.0.1");
        }
    }

    @Nested
    class GetHostNameMinusDomain {

        @Test
        void shouldReturnExactHostName_WhenDoesNotContainSubDomains() {
            var simpleHostInfo = SimpleHostInfo.from("service-host-1", "10.10.5.1");

            assertThat(MongoTests.getHostNameMinusDomain(simpleHostInfo)).isEqualTo("service-host-1");
        }

        @Test
        void shouldStripDomainFromHostName() {
            var simpleHostInfo = SimpleHostInfo.from("service-host-1.prod.acme.com", "10.10.5.1");

            assertThat(MongoTests.getHostNameMinusDomain(simpleHostInfo)).isEqualTo("service-host-1");
        }
    }
}
