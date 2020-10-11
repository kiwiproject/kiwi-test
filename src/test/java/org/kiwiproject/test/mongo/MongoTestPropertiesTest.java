package org.kiwiproject.test.mongo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.kiwiproject.test.mongo.MongoTestProperties.unitTestDatabaseName;
import static org.kiwiproject.test.mongo.MongoTestPropertiesTest.DatabaseName.assertDatabaseName;

import com.google.common.base.VerifyException;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.kiwiproject.test.mongo.MongoTestProperties.ServiceHostDomain;

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
    private static final Pattern ENDS_WITH_TIMESTAMP_PATTERN = Pattern.compile(".*_[0-9]{13,}$");

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
            assertDatabaseName(unitTestDatabaseName(FORTY_CHAR_SERVICE_NAME, FOURTEEN_CHAR_HOST_NAME))
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
            assertDatabaseName(unitTestDatabaseName(SIXTY_CHAR_SERVICE_NAME, FOURTEEN_CHAR_HOST_NAME))
                    .matchesPattern(SIXTY_CHAR_SERVICE_NAME.substring(0, 46) + "_ut_")
                    .withExpectedSize(63);
        }

        @ParameterizedTest
        @ValueSource(chars = {'/', '\\', '.', ' ', '"', '$', '*', '<', '>', ':', '|', '?'})
        void shouldReplaceInvalidCharacters(char badChar) {
            var serviceName = "test" + badChar + "service";
            assertDatabaseName(unitTestDatabaseName(serviceName, "host1"))
                    .matchesPattern("test_service_unit_test_host1_");
        }

        @Test
        void shouldReplaceMultipleInvalidCharacters() {
            var serviceName = "a/custom\\test.svc with\"bad$c*h<a>r:a|ct?ers";
            assertDatabaseName(unitTestDatabaseName(serviceName, "host42"))
                    .matchesPattern("a_custom_test_svc_with_bad_c_h_a_r_a_ct_ers_ut_");
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
        private String serviceHostMinusDomain;

        @BeforeEach
        void setUp() {
            hostName = "mongo-1.acme.com";
            port = 27_017;
            serviceName = "test-service";
            serviceHost = "service-host-1.acme.com";
            serviceHostMinusDomain = "service-host-1";
        }

        @ParameterizedTest
        @EnumSource(ServiceHostDomain.class)
        void shouldUseConstructor(ServiceHostDomain serviceHostDomain, SoftAssertions softly) {
            var properties = new MongoTestProperties(hostName, port, serviceName, serviceHost, serviceHostDomain);

            assertProperties(softly, properties, serviceHostDomain);
        }

        @ParameterizedTest
        @EnumSource(ServiceHostDomain.class)
        void shouldUseBuilder(ServiceHostDomain serviceHostDomain, SoftAssertions softly) {
            var properties = MongoTestProperties.builder()
                    .hostName(hostName)
                    .port(port)
                    .serviceName(serviceName)
                    .serviceHost(serviceHost)
                    .serviceHostDomain(serviceHostDomain)
                    .build();

            assertProperties(softly, properties, serviceHostDomain);
        }

        @Test
        void shouldDefaultToStrippingHostDomain_WhenUsingConstructor(SoftAssertions softly) {
            var properties = new MongoTestProperties(hostName, port, serviceName, serviceHost, null);

            assertProperties(softly, properties, ServiceHostDomain.STRIP);
        }

        @Test
        void shouldDefaultToStrippingHostDomain_WhenUsingBuilder(SoftAssertions softly) {
            // serviceHostDomain is not specified; it should use the default
            var properties = MongoTestProperties.builder()
                    .hostName(hostName)
                    .port(port)
                    .serviceName(serviceName)
                    .serviceHost(serviceHost)
                    .build();

            assertProperties(softly, properties, ServiceHostDomain.STRIP);
        }

        private void assertProperties(SoftAssertions softly,
                                      MongoTestProperties properties,
                                      ServiceHostDomain serviceHostDomain) {

            softly.assertThat(properties.getHostName()).isEqualTo(hostName);
            softly.assertThat(properties.getPort()).isEqualTo(port);
            softly.assertThat(properties.getServiceName()).isEqualTo(serviceName);
            softly.assertThat(properties.getServiceHostDomain()).isEqualTo(serviceHostDomain);

            var keepDomain = serviceHostDomain == ServiceHostDomain.KEEP;
            var expectedServiceHost = keepDomain ? serviceHost : serviceHostMinusDomain;
            var expectedDbNamePrefix = keepDomain ?
                    "test-service_unit_test_service-host-1_acme_com_" : "test-service_unit_test_service-host-1_";

            softly.assertThat(properties.getServiceHost()).isEqualTo(expectedServiceHost);
            softly.assertThat(properties.getDatabaseName())
                    .startsWith(expectedDbNamePrefix)
                    .matches(ENDS_WITH_TIMESTAMP_PATTERN);
            softly.assertThat(properties.getUri())
                    .startsWith("mongodb://mongo-1.acme.com:27017/" + expectedDbNamePrefix)
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

    @Nested
    class VerifyDatabaseName {

        @ParameterizedTest
        @ValueSource(strings = {
                "1234567890",
                "12345678901234567890",
                "1234567890123456789012345678901234567890",
                "123456789012345678901234567890123456789012345678901234567890",
                "123456789012345678901234567890123456789012345678901234567890123"  // 63 characters
        })
        void shouldNotThrow_givenValidDatabaseName(String databaseName) {
            assertThatCode(() -> MongoTestProperties.verifyDatabaseNameLength(databaseName))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "1234567890123456789012345678901234567890123456789012345678901234",                 // 64 characters
                "1234567890123456789012345678901234567890123456789012345678901234567890",           // 70 characters
                "12345678901234567890123456789012345678901234567890123456789012345678901234567890"  // 80 characters
        })
        void shouldThrow_VerifyException_givenInvalidDatabaseName(String databaseName) {
            assertThatThrownBy(() -> MongoTestProperties.verifyDatabaseNameLength(databaseName))
                    .isExactlyInstanceOf(VerifyException.class)
                    .hasMessage("Unexpected error: DB name must be less than 64 characters in length, but was %d: %s",
                            databaseName.length(), databaseName);
        }
    }

    @Nested
    class GetDatabaseNameWithoutTimestamp {

        @ParameterizedTest
        @CsvSource({
                "test-service, host1.acme.com, STRIP, test-service_unit_test_host1",
                "test-service, host1.acme.com, KEEP, test-service_unit_test_host1_acme_com",
                "this-is-a-service, host1.acme.com, STRIP, this-is-a-service_unit_test_host1",
                "this-is-a-service, host1.acme.com, KEEP, this-is-a-service_unit_test_host1_acme_com",
                "1234567890123456789012345678901234567890123456, host1.acme.com, STRIP, 1234567890123456789012345678901234567890123456_ut",
                "1234567890123456789012345678901234567890123456, host1.acme.com, KEEP, 1234567890123456789012345678901234567890123456_ut"
        })
        void shouldStripLastUnderscoreAndTimestamp(String serviceName,
                                                   String serviceHost,
                                                   ServiceHostDomain serviceHostDomain,
                                                   String expectedDatabaseNameWithoutTimestamp) {

            var testProperties = MongoTestProperties.builder()
                    .hostName("localhost")
                    .port(27_017)
                    .serviceName(serviceName)
                    .serviceHost(serviceHost)
                    .serviceHostDomain(serviceHostDomain)
                    .build();

            assertThat(testProperties.getDatabaseNameWithoutTimestamp())
                    .isEqualTo(expectedDatabaseNameWithoutTimestamp);
        }
    }

    @Nested
    class GetDatabaseTimestamp {

        @ParameterizedTest
        @CsvSource({
                "test-service, host1.acme.com, STRIP",
                "test-service, host1.acme.com, KEEP",
                "this-is-a-service, host1.acme.com, STRIP",
        })
        void shouldExtractTimestamp(String serviceName,
                                    String serviceHost,
                                    ServiceHostDomain serviceHostDomain) {

            var testProperties = MongoTestProperties.builder()
                    .hostName("localhost")
                    .port(27_017)
                    .serviceName(serviceName)
                    .serviceHost(serviceHost)
                    .serviceHostDomain(serviceHostDomain)
                    .build();

            // Yes, yes, the following basically does the same thing the getDatabaseTimestamp() method does,
            // but by having the test and the method with separate code, if the implementation is ever changed
            // incorrectly, then this test should fail, thereby catching the error before it gets committed.
            var databaseName = testProperties.getDatabaseName();
            var index = databaseName.lastIndexOf('_');
            var timestamp = Long.parseLong(databaseName.substring(index + 1));

            assertThat(testProperties.getDatabaseTimestamp()).isEqualTo(timestamp);
        }
    }
}
