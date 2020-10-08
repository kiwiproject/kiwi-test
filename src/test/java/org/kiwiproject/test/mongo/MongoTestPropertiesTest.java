package org.kiwiproject.test.mongo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.kiwiproject.test.mongo.MongoTestProperties.unitTestDatabaseName;
import static org.kiwiproject.test.mongo.MongoTestPropertiesTest.DatabaseName.assertDatabaseName;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.regex.Pattern;

@DisplayName("MongoTestProperties")
@ExtendWith(SoftAssertionsExtension.class)
class MongoTestPropertiesTest {

    private static final String SERVICE_NAME = "sample-service";
    private static final String FOURTEEN_CHAR_HOST_NAME = "sample-hostname";
    private static final String FOURTEEN_CHAR_HOST_NAME_PLUS_DOMAINS = "sample-hostname.acme.com";
    private static final String FIFTEEN_CHAR_SERVICE_NAME = "123456789012345";
    private static final String TWENTY_THREE_CHAR_SERVICE_NAME = "12345678901234567890123";
    private static final String TWENTY_FOUR_CHAR_SERVICE_NAME = "123456789012345678901234";
    private static final String THIRTY_NINE_CHAR_SERVICE_NAME = "123456789012345678901234567890123456789";
    private static final String FORTY_CHAR_SERVICE_NAME = "1234567890123456789012345678901234567890";
    private static final String FORTY_SIX_CHAR_SERVICE_NAME = "1234567890123456789012345678901234567890123456";
    private static final String FORTY_SEVEN_CHAR_SERVICE_NAME = "12345678901234567890123456789012345678901234567";
    private static final String SIXTY_CHAR_SERVICE_NAME = "123456789012345678901234567890123456789012345678901234567890";
    public static final Pattern ENDS_WITH_TIMESTAMP_PATTERN = Pattern.compile(".*_[0-9]{13,}$");

    @Nested
    class UnitTestDatabaseName {

        @Test
        void shouldIncludeServiceHost_givenNormalLookingNames() {
            assertDatabaseName(unitTestDatabaseName(SERVICE_NAME, FOURTEEN_CHAR_HOST_NAME))
                    .matchesPattern(SERVICE_NAME + "_unit_test_" + FOURTEEN_CHAR_HOST_NAME + "_")
                    .withExpectedSize(54);
        }

        @Test
        void shouldIncludeServiceHostSubDomainOnly_givenTwentyThreeCharacterServiceName_andFullHostName() {
            assertDatabaseName(unitTestDatabaseName(FIFTEEN_CHAR_SERVICE_NAME, FOURTEEN_CHAR_HOST_NAME_PLUS_DOMAINS))
                    .matchesPattern(FIFTEEN_CHAR_SERVICE_NAME + "_unit_test_" + FOURTEEN_CHAR_HOST_NAME + "_")
                    .withExpectedSize(55);
        }

        @Test
        void shouldIncludeServiceHost_givenTwentyThreeCharacterServiceName() {
            assertDatabaseName(unitTestDatabaseName(TWENTY_THREE_CHAR_SERVICE_NAME, FOURTEEN_CHAR_HOST_NAME))
                    .matchesPattern(TWENTY_THREE_CHAR_SERVICE_NAME + "_unit_test_" + FOURTEEN_CHAR_HOST_NAME + "_")
                    .withExpectedSize(63);
        }

        @Test
        void shouldNotIncludeServiceHost_givenTwentyFourCharacterServiceName() {
            assertDatabaseName(unitTestDatabaseName(TWENTY_FOUR_CHAR_SERVICE_NAME, FOURTEEN_CHAR_HOST_NAME))
                    .matchesPattern(TWENTY_FOUR_CHAR_SERVICE_NAME + "_unit_test_")
                    .withExpectedSize(48);
        }

        @Test
        void shouldNotIncludeServiceHost_givenThirtyNineCharacterServiceName() {
            assertDatabaseName(unitTestDatabaseName(THIRTY_NINE_CHAR_SERVICE_NAME, FOURTEEN_CHAR_HOST_NAME))
                    .matchesPattern(THIRTY_NINE_CHAR_SERVICE_NAME + "_unit_test_")
                    .withExpectedSize(63);
        }

        @Test
        void shouldNotIncludeServiceHost_andShouldUseShortId_givenFortyCharacterServiceName() {
            assertDatabaseName(unitTestDatabaseName(FORTY_CHAR_SERVICE_NAME,
                    FOURTEEN_CHAR_HOST_NAME))
                    .matchesPattern(FORTY_CHAR_SERVICE_NAME + "_ut_")
                    .withExpectedSize(57);
        }

        @Test
        void shouldNotIncludeServiceHost_andShouldUseShortId_givenFortySixCharacterServiceName() {
            assertDatabaseName(unitTestDatabaseName(FORTY_SIX_CHAR_SERVICE_NAME, FOURTEEN_CHAR_HOST_NAME))
                    .matchesPattern(FORTY_SIX_CHAR_SERVICE_NAME + "_ut_")
                    .withExpectedSize(63);
        }

        @Test
        void shouldNotIncludeServiceHost_andShouldTrimServiceName_andShouldUseShortId_givenFortySevenCharacterServiceName() {
            assertDatabaseName(unitTestDatabaseName(FORTY_SEVEN_CHAR_SERVICE_NAME, FOURTEEN_CHAR_HOST_NAME))
                    .matchesPattern(FORTY_SEVEN_CHAR_SERVICE_NAME.substring(0, 46) + "_ut_")
                    .withExpectedSize(63);
        }

        @Test
        void shouldNotIncludeServiceHost_andShouldTrimServiceName_andShouldUseShortId_givenSixtyCharacterServiceName() {
            assertDatabaseName(unitTestDatabaseName(SIXTY_CHAR_SERVICE_NAME,
                    FOURTEEN_CHAR_HOST_NAME))
                    .matchesPattern(SIXTY_CHAR_SERVICE_NAME.substring(0, 46) + "_ut_")
                    .withExpectedSize(63);
        }
    }

    static final class DatabaseName {

        private final String databaseName;

        private DatabaseName(String databaseName) {
            this.databaseName = databaseName;
        }

        static DatabaseName assertDatabaseName(String databaseName) {
            return new DatabaseName(databaseName);
        }

        DatabaseName matchesPattern(String expectedPattern) {
            assertThat(databaseName)
                    .startsWith(expectedPattern)
                    .matches(Pattern.compile(expectedPattern + "[0-9]{13,}$"));
            return this;
        }

        void withExpectedSize(int expectedSize) {
            assertThat(databaseName).hasSize(expectedSize);
        }
    }

    @Nested
    class Construction {

        private String hostName;
        private int port;
        private String serviceName;
        private String serviceHost;

        @BeforeEach
        void setUp() {
            hostName = "mongo-1.acme.com";
            port = 27_017;
            serviceName = "test-service";
            serviceHost = "service-host-1.acme.com";
        }

        @Test
        void shouldSupportConstructor(SoftAssertions softly) {
            var properties = new MongoTestProperties(hostName, port, serviceName, serviceHost);

            assertProperties(softly, properties);
        }

        @Test
        void shouldSupportBuilder(SoftAssertions softly) {
            var properties = MongoTestProperties.builder()
                    .hostName(hostName)
                    .port(port)
                    .serviceName(serviceName)
                    .serviceHost(serviceHost)
                    .build();

            assertProperties(softly, properties);
        }

        private void assertProperties(SoftAssertions softly, MongoTestProperties properties) {
            softly.assertThat(properties.getHostName()).isEqualTo(hostName);
            softly.assertThat(properties.getPort()).isEqualTo(port);
            softly.assertThat(properties.getServiceName()).isEqualTo(serviceName);
            softly.assertThat(properties.getServiceHost()).isEqualTo(serviceHost);
            softly.assertThat(properties.getDatabaseName())
                    .startsWith("test-service_unit_test_service-host-1.acme.com_")
                    .matches(ENDS_WITH_TIMESTAMP_PATTERN);
            softly.assertThat(properties.getUri())
                    .startsWith("mongodb://mongo-1.acme.com:27017/test-service_unit_test_service-host-1.acme.com_")
                    .matches(ENDS_WITH_TIMESTAMP_PATTERN);
        }
    }

    @Test
    void shouldSupportMultipleHostNames() {
        var properties = MongoTestProperties.builder()
                .hostName(" mongo1.test, mongo2.test ")
                .port(27_017)
                .serviceName("test-service")
                .serviceHost("svc-host-1")
                .build();

        assertThat(properties.getUri())
                .startsWith("mongodb://mongo1.test:27017,mongo2.test:27017/test-service_unit_test_svc-host-1_")
                .matches(Pattern.compile(".*_[0-9]{13,}\\?replicaSet=rs0$"));
    }
}
