package org.kiwiproject.test.junit.jupiter;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.kiwiproject.test.junit.jupiter.MongoDbExtensionTestHelpers.assertNoDataInCollections;
import static org.kiwiproject.test.junit.jupiter.MongoDbExtensionTestHelpers.assertNoDocumentsAndInsertFirstDocument;
import static org.kiwiproject.test.junit.jupiter.MongoDbExtensionTestHelpers.assertNoDocumentsAndInsertSecondDocument;
import static org.kiwiproject.test.junit.jupiter.MongoDbExtensionTestHelpers.assertTestDatabaseExists;
import static org.kiwiproject.test.junit.jupiter.MongoDbExtensionTestHelpers.assertTestDatabaseExistsWhenTestHasTag;
import static org.kiwiproject.test.junit.jupiter.MongoDbExtensionTestHelpers.buildMongoTestProperties;
import static org.kiwiproject.test.junit.jupiter.MongoTestContainerHelpers.startedMongoDBContainer;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.kiwiproject.test.mongo.MongoTestProperties;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

@DisplayName("MongoDbExtension: Drop @AfterAll")
@Testcontainers(disabledWithoutDocker = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MongoDbExtensionDropAfterAllTest {

    @Container
    static final MongoDBContainer MONGODB = startedMongoDBContainer();

    private static final MongoTestProperties TEST_PROPERTIES = buildMongoTestProperties(MONGODB);

    private static MongoClient client;

    @BeforeAll
    static void beforeAll() {
        client = MongoClients.create(TEST_PROPERTIES.getUri());
    }

    @SuppressWarnings("unused")
    @RegisterExtension
    static final MongoDbExtension MONGO_DB_EXTENSION = new MongoDbExtension(TEST_PROPERTIES);

    @BeforeEach
    void setUp(TestInfo testInfo) {
        assertTestDatabaseExistsWhenTestHasTag(client, TEST_PROPERTIES, testInfo, "dbShouldExistBefore");

        // Any data in collections should have been cleaned up by @AfterEach
        assertNoDataInCollections(client, TEST_PROPERTIES);
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        assertTestDatabaseExistsWhenTestHasTag(client, TEST_PROPERTIES, testInfo, "dbShouldExistAfter");
    }

    @AfterAll
    static void afterAll() {
        // we should still see the database, since our @AfterAll runs before the extension's
        assertTestDatabaseExists(client, TEST_PROPERTIES);
    }

    @Test
    @Order(1)
    void shouldListCollections() {
        var mongoDatabase = client.getDatabase(TEST_PROPERTIES.getDatabaseName());
        assertThat(newArrayList(mongoDatabase.listCollectionNames().iterator())).isEmpty();
    }

    @Test
    @Order(2)
    @Tag("dbShouldExistAfter")
    void shouldInsertSomething() {
        assertNoDocumentsAndInsertFirstDocument(client, TEST_PROPERTIES);
    }

    @Test
    @Tag("dbShouldExistBefore")
    @Tag("dbShouldExistAfter")
    @Order(3)
    void shouldInsertSomethingElse() {
        assertNoDocumentsAndInsertSecondDocument(client, TEST_PROPERTIES);
    }
}
