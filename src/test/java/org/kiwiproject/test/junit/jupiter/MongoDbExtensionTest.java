package org.kiwiproject.test.junit.jupiter;

import static com.google.common.base.Verify.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.kiwiproject.test.junit.jupiter.MongoDbTestHelpers.buildMongoTestProperties;
import static org.kiwiproject.test.junit.jupiter.MongoDbTestHelpers.startInMemoryMongoServer;

import com.mongodb.MongoClient;
import de.bwaldvogel.mongo.MongoServer;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.bson.Document;
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

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@DisplayName("MongoDbExtension")
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
    class Constructor {

        @Test
        void shouldCreateWithTestProperties(SoftAssertions softly) {
            var extension = new MongoDbExtension(testProperties);

            softly.assertThat(extension.getDropTime()).isEqualTo(MongoDbExtension.DropTime.AFTER_ALL);
            softly.assertThat(extension.getCleanupOption()).isEqualTo(MongoDbExtension.CleanupOption.REMOVE_RECORDS);
            softly.assertThat(extension.isSkipDatabaseCleanup()).isFalse();
        }

        @Test
        void shouldCreateWithTestPropertiesAndDropTime(SoftAssertions softly) {
            var extension = new MongoDbExtension(testProperties, MongoDbExtension.DropTime.AFTER_EACH);

            softly.assertThat(extension.getDropTime()).isEqualTo(MongoDbExtension.DropTime.AFTER_EACH);
            softly.assertThat(extension.getCleanupOption()).isEqualTo(MongoDbExtension.CleanupOption.REMOVE_RECORDS);
            softly.assertThat(extension.isSkipDatabaseCleanup()).isFalse();
        }

        @Test
        void shouldCreateWithTestPropertiesAndDropTimeAndCleanupOption(SoftAssertions softly) {
            var extension = new MongoDbExtension(testProperties, MongoDbExtension.DropTime.AFTER_EACH, MongoDbExtension.CleanupOption.REMOVE_COLLECTION);

            softly.assertThat(extension.getDropTime()).isEqualTo(MongoDbExtension.DropTime.AFTER_EACH);
            softly.assertThat(extension.getCleanupOption()).isEqualTo(MongoDbExtension.CleanupOption.REMOVE_COLLECTION);
            softly.assertThat(extension.isSkipDatabaseCleanup()).isFalse();
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
            softly.assertThat(extension.isSkipDatabaseCleanup()).isFalse();
            softly.assertThat(extension.getDatabaseCleanupThreshold()).hasMinutes(10);
        }

        @Test
        void shouldCreateWithExplicitOptions(SoftAssertions softly) {
            var extension = MongoDbExtension.builder()
                    .props(testProperties)
                    .dropTime(MongoDbExtension.DropTime.BEFORE_EACH)
                    .cleanupOption(MongoDbExtension.CleanupOption.REMOVE_COLLECTION)
                    .skipDatabaseCleanup(true)
                    .build();

            softly.assertThat(extension.getDropTime()).isEqualTo(MongoDbExtension.DropTime.BEFORE_EACH);
            softly.assertThat(extension.getCleanupOption()).isEqualTo(MongoDbExtension.CleanupOption.REMOVE_COLLECTION);
            softly.assertThat(extension.isSkipDatabaseCleanup()).isTrue();
        }

        @Test
        void shouldCreateWithExplicitDatabaseCleanupThreshold(SoftAssertions softly) {
            var extension = MongoDbExtension.builder()
                    .props(testProperties)
                    .dropTime(MongoDbExtension.DropTime.NEVER)
                    .cleanupOption(MongoDbExtension.CleanupOption.REMOVE_NEVER)
                    .databaseCleanupThreshold(Duration.ofMinutes(30))
                    .build();

            softly.assertThat(extension.getDropTime()).isEqualTo(MongoDbExtension.DropTime.NEVER);
            softly.assertThat(extension.getCleanupOption()).isEqualTo(MongoDbExtension.CleanupOption.REMOVE_NEVER);
            softly.assertThat(extension.isSkipDatabaseCleanup()).isFalse();
            softly.assertThat(extension.getDatabaseCleanupThreshold()).hasMinutes(30);
        }
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
                "test-service_unit_test_host1_1605410622974",
                "another-service_unit_test_localhost_1605410631247",
                "yet-another-service_unit_test_localhost_1605410638221"
        })
        void shouldBeFalse_WhenDatabaseName_DoesNotContainTestDatabaseNameWithoutTimestamp(String databaseName) {
            verifyTestPropertiesDatabaseName();

            assertThat(MongoDbExtension.isUnitTestDatabaseForThisService(databaseName, testProperties))
                    .isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "admin",
                "local",
                "test",
                "customer_database"
        })
        void shouldBeFalse_WhenDatabaseName_IsNotInOurExpectedFormat(String databaseName) {
            verifyTestPropertiesDatabaseName();

            assertThat(MongoDbExtension.isUnitTestDatabaseForThisService(databaseName, testProperties))
                    .isFalse();
        }

        private void verifyTestPropertiesDatabaseName() {
            var testPropsDbName = testProperties.getDatabaseNameWithoutTimestamp();
            verify(testPropsDbName.equals("test-service_unit_test_localhost"),
                    "Expected testProperties database name (w/o timestamp) to be: test-service_unit_test_localhost but was: %s",
                    testPropsDbName);
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
    class WhenDatabasesFromPriorTestRunsExist {

        private MongoClient mongoClient;
        private String customerDatabaseName;
        private String orderDatabaseName;
        private String marketingDatabaseName;
        private String expiredDatabaseName1;
        private String expiredDatabaseName2;
        private String notExpiredDatabaseName1;
        private String notExpiredDatabaseName2;

        @BeforeEach
        void setUp() {
            mongoClient = testProperties.newMongoClient();

            customerDatabaseName = createDatabase("customer_database");
            orderDatabaseName = createDatabase("order_database");
            marketingDatabaseName = createDatabase("marketing_database");
            expiredDatabaseName1 = createDatabaseTimestampedMinutesBefore(testProperties, 11);
            expiredDatabaseName2 = createDatabaseTimestampedMinutesBefore(testProperties, 10);
            notExpiredDatabaseName1 = createDatabaseTimestampedMinutesBefore(testProperties, 9);
            notExpiredDatabaseName2 = createDatabaseTimestampedMinutesBefore(testProperties, 1);
        }

        private String createDatabaseTimestampedMinutesBefore(MongoTestProperties testProperties, int minutesToSubtract) {
            var databaseName = databaseNameTimestampedMinutesBefore(testProperties, minutesToSubtract);
            return createDatabase(databaseName);
        }

        private String createDatabase(String databaseName) {
            var database = mongoClient.getDatabase(databaseName);
            database.getCollection("test_collection").insertOne(new Document());
            return databaseName;
        }

        private String databaseNameTimestampedMinutesBefore(MongoTestProperties testProperties, int minutesToSubtract) {
            var baseDatabaseName = testProperties.getDatabaseNameWithoutTimestamp();
            var originalTimestamp = testProperties.getDatabaseTimestamp();
            var newTimestamp = originalTimestamp - TimeUnit.MINUTES.toMillis(minutesToSubtract);
            return baseDatabaseName + "_" + newTimestamp;
        }

        @Test
        void shouldCleanupDatabasesOlderThanThreshold() {
            new MongoDbExtension(testProperties);
            var databaseNames = MongoDbTestHelpers.databaseNames(mongoClient);

            assertThat(databaseNames)
                    .contains(customerDatabaseName, orderDatabaseName, marketingDatabaseName)
                    .contains(notExpiredDatabaseName1, notExpiredDatabaseName2)
                    .doesNotContain(expiredDatabaseName1, expiredDatabaseName2);
        }

        @Test
        void shouldCleanupOldDatabases_WhenMakeDefaultCleanupThreshold_BeforeAllDatabases() {
            MongoDbExtension.builder()
                    .props(testProperties)
                    .databaseCleanupThreshold(Duration.ofSeconds(45))
                    .build();

            var databaseNames = MongoDbTestHelpers.databaseNames(mongoClient);
            assertThat(databaseNames)
                    .containsExactlyInAnyOrder(customerDatabaseName, orderDatabaseName, marketingDatabaseName);
        }

        @Test
        void shouldNotCleanupOldDatabases_WhenMakeDefaultCleanupThreshold_AfterAllDatabases() {
            MongoDbExtension.builder()
                    .props(testProperties)
                    .databaseCleanupThreshold(Duration.ofMinutes(45))
                    .build();

            var databaseNames = MongoDbTestHelpers.databaseNames(mongoClient);
            assertThat(databaseNames).contains(
                    notExpiredDatabaseName1, notExpiredDatabaseName2, expiredDatabaseName1, expiredDatabaseName2);
        }

        @Test
        void shouldNotCleanupOldDatabases_WhenSkipDatabaseCleanupOptionIsTrue() {
            MongoDbExtension.builder()
                    .props(testProperties)
                    .skipDatabaseCleanup(true)
                    .build();

            var databaseNames = MongoDbTestHelpers.databaseNames(mongoClient);
            assertThat(databaseNames)
                    .contains(customerDatabaseName, orderDatabaseName, marketingDatabaseName)
                    .contains(notExpiredDatabaseName1, notExpiredDatabaseName2, expiredDatabaseName1, expiredDatabaseName2);
        }
    }
}
