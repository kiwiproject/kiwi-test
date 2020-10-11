package org.kiwiproject.test.junit.jupiter;

import static com.google.common.base.Verify.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.kiwiproject.test.junit.jupiter.MongoDbTestHelpers.buildMongoTestProperties;
import static org.kiwiproject.test.junit.jupiter.MongoDbTestHelpers.startInMemoryMongoServer;

import de.bwaldvogel.mongo.MongoServer;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.kiwiproject.test.mongo.MongoTestProperties;
import org.kiwiproject.test.mongo.MongoTestProperties.ServiceHostDomain;

@DisplayName("MongoDbExtension: Construction")
@ExtendWith(SoftAssertionsExtension.class)
class MongoDbExtensionTest {

    private MongoServer mongoServer;
    private MongoTestProperties testProperties;

    @BeforeEach
    void setUp() {
        mongoServer = startInMemoryMongoServer();
        testProperties = buildMongoTestProperties(mongoServer.getLocalAddress());
    }

    @AfterEach
    void tearDown() {
        mongoServer.shutdownNow();
    }

    @Test
    void shouldSetExtensionProperties(SoftAssertions softly) {
        var extension = new MongoDbExtension(testProperties);

        softly.assertThat(extension.getProps()).isNotNull();
        softly.assertThat(extension.getMongo()).isNotNull();
        softly.assertThat(extension.getMongoUri()).isNotBlank();
        softly.assertThat(extension.getDatabaseName()).isNotBlank();
    }

    @Nested
    class IsUnitTestDatabaseForThisService {

        @ParameterizedTest
        @CsvSource({
                "test-service, host1.acme.com, STRIP",
                "test-service, host1.acme.com, KEEP",
                "this-is-a-service, host1.acme.com, STRIP",
                "this-is-a-service, host1.acme.com, KEEP",
                "this-is-a-really-really-really-really-really-really-long-service, host1.acme.com, KEEP",
                "1234567890123456789012345678901234567890123456, host1.acme.com, STRIP",
                "1234567890123456789012345678901234567890123456, host1.acme.com, KEEP"
        })
        void shouldBeTrue_WhenDatabaseName_ContainsTestDatabaseNameWithoutTimestamp(
                String serviceName,
                String serviceHost,
                ServiceHostDomain serviceHostDomain) {

            var testProps = MongoTestProperties.builder()
                    .hostName("localhost")
                    .port(27_017)
                    .serviceName(serviceName)
                    .serviceHost(serviceHost)
                    .serviceHostDomain(serviceHostDomain)
                    .build();

            assertThat(MongoDbExtension.isUnitTestDatabaseForThisService(testProps.getDatabaseName(), testProps))
                    .isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "test-service_unit_test_host1",
                "another-service_unit_test_localhost",
                "yet-another-service_unit_test_localhost"
        })
        void shouldBeFalse_WhenDatabaseName_DoesNotContainTestDatabaseNameWithoutTimestamp(String databaseName) {
            var testPropsDbName = testProperties.getDatabaseNameWithoutTimestamp();
            verify(testPropsDbName.equals("test-service_unit_test_localhost"),
                    "Expected testProperties database name (w/o timestamp) to be: test-service_unit_test_localhost but was: %s",
                    testPropsDbName);

            assertThat(MongoDbExtension.isUnitTestDatabaseForThisService(databaseName, testProperties))
                    .isFalse();
        }
    }

    @Nested
    class DatabaseIsOlderThanThreshold {

        @ParameterizedTest
        @CsvSource({
                "1602375491862, test-service_unit_test_host1_1602375491864, false",  // db created before threshold
                "1602375491863, test-service_unit_test_host1_1602375491864, false",  // db created before threshold
                "1602375491864, test-service_unit_test_host1_1602375491864, true",   // db created at threshold
                "1602375491865, test-service_unit_test_host1_1602375491864, true",   // db created after threshold
                "1602375491866, test-service_unit_test_host1_1602375491864, true",   // db created after threshold
        })
        void shouldCompareDatabaseTimestampToKeepThreshold(long keepThresholdMillis,
                                                           String databaseName,
                                                           boolean expectedOlderThan) {

            assertThat(MongoDbExtension.databaseIsOlderThanThreshold(databaseName, keepThresholdMillis))
                    .isEqualTo(expectedOlderThan);
        }
    }

    @Nested
    class Constructor {

        @Test
        void shouldCreateWithTestProperties(SoftAssertions softly) {
            var extension = new MongoDbExtension(testProperties);

            softly.assertThat(extension.getDropTime()).isEqualTo(MongoDbExtension.DropTime.AFTER_ALL);
            softly.assertThat(extension.getCleanupOption()).isEqualTo(MongoDbExtension.CleanupOption.REMOVE_RECORDS);
            softly.assertThat(extension.isSkipCleanup()).isFalse();
        }

        @Test
        void shouldCreateWithTestPropertiesAndDropTime(SoftAssertions softly) {
            var extension = new MongoDbExtension(testProperties, MongoDbExtension.DropTime.AFTER_EACH);

            softly.assertThat(extension.getDropTime()).isEqualTo(MongoDbExtension.DropTime.AFTER_EACH);
            softly.assertThat(extension.getCleanupOption()).isEqualTo(MongoDbExtension.CleanupOption.REMOVE_RECORDS);
            softly.assertThat(extension.isSkipCleanup()).isFalse();
        }

        @Test
        void shouldCreateWithTestPropertiesAndDropTimeAndCleanupOption(SoftAssertions softly) {
            var extension = new MongoDbExtension(testProperties, MongoDbExtension.DropTime.AFTER_EACH, MongoDbExtension.CleanupOption.REMOVE_COLLECTION);

            softly.assertThat(extension.getDropTime()).isEqualTo(MongoDbExtension.DropTime.AFTER_EACH);
            softly.assertThat(extension.getCleanupOption()).isEqualTo(MongoDbExtension.CleanupOption.REMOVE_COLLECTION);
            softly.assertThat(extension.isSkipCleanup()).isFalse();
        }
    }

    @Nested
    class Builder {

        @Test
        void shouldCreateWithDefaults(SoftAssertions softly) {
            var extension = MongoDbExtension.builder()
                    .props(testProperties)
                    .build();

            softly.assertThat(extension.getDropTime()).isEqualTo(MongoDbExtension.DropTime.AFTER_ALL);
            softly.assertThat(extension.getCleanupOption()).isEqualTo(MongoDbExtension.CleanupOption.REMOVE_RECORDS);
            softly.assertThat(extension.isSkipCleanup()).isFalse();
        }

        @Test
        void shouldCreateWithExplicitOptions(SoftAssertions softly) {
            var extension = MongoDbExtension.builder()
                    .props(testProperties)
                    .dropTime(MongoDbExtension.DropTime.BEFORE_EACH)
                    .cleanupOption(MongoDbExtension.CleanupOption.REMOVE_COLLECTION)
                    .skipCleanup(true)
                    .build();

            softly.assertThat(extension.getDropTime()).isEqualTo(MongoDbExtension.DropTime.BEFORE_EACH);
            softly.assertThat(extension.getCleanupOption()).isEqualTo(MongoDbExtension.CleanupOption.REMOVE_COLLECTION);
            softly.assertThat(extension.isSkipCleanup()).isTrue();
        }
    }
}
