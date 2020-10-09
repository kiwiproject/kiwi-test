package org.kiwiproject.test.junit.jupiter;

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
import org.kiwiproject.test.mongo.MongoTestProperties;

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

        // TODO...
    }

    @Nested
    class DatabaseIsOlderThanThreshold {

        // TODO...
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
