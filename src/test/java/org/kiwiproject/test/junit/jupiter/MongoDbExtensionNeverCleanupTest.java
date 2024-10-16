package org.kiwiproject.test.junit.jupiter;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.kiwiproject.collect.KiwiLists.first;
import static org.kiwiproject.test.junit.jupiter.MongoDbExtensionTestHelpers.assertNoDocumentsAndInsertFirstDocument;
import static org.kiwiproject.test.junit.jupiter.MongoDbExtensionTestHelpers.assertTestDatabaseExists;
import static org.kiwiproject.test.junit.jupiter.MongoDbExtensionTestHelpers.assertTestDatabaseExistsWhenTestHasTag;
import static org.kiwiproject.test.junit.jupiter.MongoDbExtensionTestHelpers.buildMongoTestProperties;
import static org.kiwiproject.test.junit.jupiter.MongoDbExtensionTestHelpers.docToInsertFirst;
import static org.kiwiproject.test.junit.jupiter.MongoDbExtensionTestHelpers.docToInsertSecond;
import static org.kiwiproject.test.junit.jupiter.MongoDbExtensionTestHelpers.findAllDocuments;
import static org.kiwiproject.test.junit.jupiter.MongoDbExtensionTestHelpers.findFirstDocument;
import static org.kiwiproject.test.junit.jupiter.MongoDbExtensionTestHelpers.findSecondDocument;
import static org.kiwiproject.test.junit.jupiter.MongoDbExtensionTestHelpers.getMongoDatabase;
import static org.kiwiproject.test.junit.jupiter.MongoDbExtensionTestHelpers.getTestCollection;
import static org.kiwiproject.test.junit.jupiter.MongoDbExtensionTestHelpers.insertSecondDocument;
import static org.kiwiproject.test.junit.jupiter.MongoTestContainerHelpers.startedMongoDBContainer;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
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
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DisplayName("MongoDbExtension: Never Cleanup Collections")
@Testcontainers(disabledWithoutDocker = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MongoDbExtensionNeverCleanupTest {

    @Container
    static final MongoDBContainer MONGODB = startedMongoDBContainer();

    private static final MongoTestProperties TEST_PROPERTIES = buildMongoTestProperties(MONGODB);

    private static MongoClient client;

    private MongoDatabase mongoDatabase;

    @BeforeAll
    static void beforeAll() {
        client = MongoClients.create(TEST_PROPERTIES.getUri());
    }

    @SuppressWarnings("unused")
    @RegisterExtension
    static final MongoDbExtension MONGO_DB_EXTENSION = MongoDbExtension.builder()
            .props(TEST_PROPERTIES)
            .dropTime(MongoDbExtension.DropTime.AFTER_ALL)  // ensure collection cleanup method is called in @AfterEach
            .cleanupOption(MongoDbExtension.CleanupOption.REMOVE_NEVER)
            .skipDatabaseCleanup(true)
            .build();

    @BeforeEach
    void setUp(TestInfo testInfo) {
        assertTestDatabaseExistsWhenTestHasTag(client, TEST_PROPERTIES, testInfo, "dbShouldExistBefore");

        mongoDatabase = getMongoDatabase(client, TEST_PROPERTIES);
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        assertTestDatabaseExistsWhenTestHasTag(client, TEST_PROPERTIES, testInfo, "dbShouldExistAfter");
    }

    @AfterAll
    static void afterAll() {
        // we should still see the database, since our @AfterAll runs before the extension's
        assertTestDatabaseExists(client, TEST_PROPERTIES);

        // we should still see the second document
        var mongoDatabase = getMongoDatabase(client, TEST_PROPERTIES);
        var collection = getTestCollection(mongoDatabase);
        var documents = findAllDocuments(collection);
        assertThat(documents).hasSize(1);
    }

    @Test
    @Order(1)
    void shouldListCollections() {
        var theMongoDatabase = client.getDatabase(TEST_PROPERTIES.getDatabaseName());
        assertThat(newArrayList(theMongoDatabase.listCollectionNames().iterator())).isEmpty();
    }

    @Test
    @Order(2)
    @Tag("dbShouldExistAfter")
    void shouldInsertSomething() {
        assertNoDocumentsAndInsertFirstDocument(client, TEST_PROPERTIES);
    }

    @Test
    @Order(3)
    @Tag("dbShouldExistBefore")
    @Tag("dbShouldExistAfter")
    void shouldSeeDocumentsFromPreviousTestAndInsertSomethingElse() {
        var collection = getTestCollection(mongoDatabase);
        var documents = findAllDocuments(collection);
        assertThat(documents).hasSize(1);

        var document = first(documents);
        assertContainsAllExpectedProperties(document, docToInsertFirst());

        insertSecondDocument(collection);
    }

    @Test
    @Order(4)
    @Tag("dbShouldExistBefore")
    @Tag("dbShouldExistAfter")
    void shouldSeeDocumentsFromPreviousTests() {
        var collection = getTestCollection(mongoDatabase);
        var documents = findAllDocuments(collection);
        assertThat(documents).hasSize(2);

        var firstDoc = findFirstDocument(documents);
        assertContainsAllExpectedProperties(firstDoc, docToInsertFirst());

        var secondDoc = findSecondDocument(documents);
        assertContainsAllExpectedProperties(secondDoc, docToInsertSecond());
    }

    @Test
    @Order(5)
    @Tag("dbShouldExistBefore")
    @Tag("dbShouldExistAfter")
    void shouldSeeDocumentsFromPreviousTestsAndDeleteOneDocument() {
        var collection = getTestCollection(mongoDatabase);
        var documents = findAllDocuments(collection);
        assertThat(documents).hasSize(2);

        var deleteResult = collection.deleteOne(Filters.eq("foo", "bar"));
        assertThat(deleteResult.getDeletedCount())
                .describedAs("The first document should have been deleted")
                .isOne();
    }

    @Test
    @Order(6)
    @Tag("dbShouldExistBefore")
    @Tag("dbShouldExistAfter")
    void shouldSeeRemainingDocumentsFromPreviousTests() {
        var collection = getTestCollection(mongoDatabase);
        var documents = findAllDocuments(collection);
        assertThat(documents).hasSize(1);

        assertThatCode(() -> findSecondDocument(documents))
                .describedAs("The second document should still exist but was not found")
                .doesNotThrowAnyException();
    }

    private void assertContainsAllExpectedProperties(Document actualDoc, Document expectedDoc) {
        assertThat(actualDoc.entrySet()).containsAll(expectedDoc.entrySet());
    }
}
