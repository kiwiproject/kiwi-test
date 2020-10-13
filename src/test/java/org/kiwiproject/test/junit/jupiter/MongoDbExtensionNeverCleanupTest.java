package org.kiwiproject.test.junit.jupiter;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.kiwiproject.collect.KiwiLists.first;
import static org.kiwiproject.test.junit.jupiter.MongoDbTestHelpers.assertNoDocumentsAndInsertFirstDocument;
import static org.kiwiproject.test.junit.jupiter.MongoDbTestHelpers.assertNoTestDatabaseExists;
import static org.kiwiproject.test.junit.jupiter.MongoDbTestHelpers.assertTestDatabaseExists;
import static org.kiwiproject.test.junit.jupiter.MongoDbTestHelpers.buildMongoTestProperties;
import static org.kiwiproject.test.junit.jupiter.MongoDbTestHelpers.docToInsertFirst;
import static org.kiwiproject.test.junit.jupiter.MongoDbTestHelpers.docToInsertSecond;
import static org.kiwiproject.test.junit.jupiter.MongoDbTestHelpers.findAllDocuments;
import static org.kiwiproject.test.junit.jupiter.MongoDbTestHelpers.findFirstDocument;
import static org.kiwiproject.test.junit.jupiter.MongoDbTestHelpers.findSecondDocument;
import static org.kiwiproject.test.junit.jupiter.MongoDbTestHelpers.getMongoDatabase;
import static org.kiwiproject.test.junit.jupiter.MongoDbTestHelpers.getTestCollection;
import static org.kiwiproject.test.junit.jupiter.MongoDbTestHelpers.insertSecondDocument;
import static org.kiwiproject.test.junit.jupiter.MongoDbTestHelpers.startInMemoryMongoServer;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import de.bwaldvogel.mongo.MongoServer;
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

import java.net.InetSocketAddress;

@DisplayName("MongoDbExtension: Never Cleanup Collections")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MongoDbExtensionNeverCleanupTest {

    private static final MongoServer MONGO_SERVER = startInMemoryMongoServer();
    private static final InetSocketAddress INET_SOCKET_ADDRESS = MONGO_SERVER.getLocalAddress();
    private static final MongoTestProperties TEST_PROPERTIES = buildMongoTestProperties(INET_SOCKET_ADDRESS);

    private static MongoClient client;

    private MongoDatabase mongoDatabase;

    @BeforeAll
    static void beforeAll() {
        client = new MongoClient(new ServerAddress(INET_SOCKET_ADDRESS));
    }

    @SuppressWarnings("unused")
    @RegisterExtension
    static final MongoDbExtension MONGO_DB_EXTENSION = MongoDbExtension.builder()
            .props(TEST_PROPERTIES)
            .dropTime(MongoDbExtension.DropTime.AFTER_ALL)  // ensure collection cleanup method is called in @AfterEach
            .cleanupOption(MongoDbExtension.CleanupOption.REMOVE_NEVER)
            .skipCleanup(true)
            .build();

    @BeforeEach
    void setUp(TestInfo testInfo) {
        if (testInfo.getTags().contains("firstTest")) {
            assertNoTestDatabaseExists(client);
        } else {
            assertTestDatabaseExists(client, TEST_PROPERTIES);
            mongoDatabase = getMongoDatabase(client, TEST_PROPERTIES);
        }
    }

    @AfterEach
    void tearDown() {
        assertTestDatabaseExists(client, TEST_PROPERTIES);
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
    @Tag("firstTest")
    void shouldListCollections() {
        var mongoDatabase = client.getDatabase(TEST_PROPERTIES.getDatabaseName());
        assertThat(newArrayList(mongoDatabase.listCollectionNames().iterator())).isEmpty();
    }

    @Test
    @Order(2)
    void shouldInsertSomething() {
        assertNoDocumentsAndInsertFirstDocument(client, TEST_PROPERTIES);
    }

    @Test
    @Order(3)
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
