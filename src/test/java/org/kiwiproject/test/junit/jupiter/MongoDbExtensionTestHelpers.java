package org.kiwiproject.test.junit.jupiter;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lombok.experimental.UtilityClass;
import org.bson.Document;
import org.junit.jupiter.api.TestInfo;
import org.kiwiproject.test.mongo.MongoTestProperties;
import org.testcontainers.containers.MongoDBContainer;

import java.util.List;

@UtilityClass
class MongoDbExtensionTestHelpers {

    public static final String TEST_COLLECTION_NAME = "test_collection";

    public static final int STANDARD_MONGODB_PORT = 27_017;

    static MongoTestProperties buildMongoTestProperties(MongoDBContainer mongoContainer) {
        var host = mongoContainer.getHost();
        var port = mongoContainer.getMappedPort(STANDARD_MONGODB_PORT);
        return buildMongoTestProperties(host, port);
    }

    static MongoTestProperties buildMongoTestProperties(String hostName, int port) {
        return MongoTestProperties.builder()
                .hostName(hostName)
                .port(port)
                .serviceName("test-service")
                .serviceHost("localhost")
                .build();
    }

    static void assertTestDatabaseExistsWhenTestHasTag(MongoClient client,
                                                       MongoTestProperties testProperties,
                                                       TestInfo testInfo,
                                                       String tag) {

        if (testInfo.getTags().contains(tag)) {
            assertTestDatabaseExists(client, testProperties);
        } else {
            assertNoTestDatabaseExists(client, testProperties);
        }
    }

    static void assertTestDatabaseExists(MongoClient client, MongoTestProperties testProperties) {
        assertThat(databaseNames(client)).contains(testProperties.getDatabaseName());
    }

    static void assertNoTestDatabaseExists(MongoClient client, MongoTestProperties testProperties) {
        assertThat(databaseNames(client)).doesNotContain(testProperties.getDatabaseName());
    }

    static void assertNoTestDatabaseExists(MongoClient client) {
        assertThat(databaseNames(client)).isEmpty();
    }

    static void assertNoDataInCollections(MongoClient client, MongoTestProperties testProperties) {
        var database = getMongoDatabase(client, testProperties);
        var collectionNameIterator = database
                .listCollectionNames()
                .iterator();

        var collectionNames = newArrayList(collectionNameIterator);

        collectionNames.stream()
                .filter(collectionName -> !"system.indexes".equals(collectionName))
                .forEach(collection -> assertThat(database.getCollection(collection).countDocuments()).isZero());
    }

    static List<String> databaseNames(MongoClient client) {
        return ImmutableList.copyOf(client.listDatabaseNames());
    }

    static void assertNoDocumentsAndInsertFirstDocument(MongoClient client, MongoTestProperties testProperties) {
        var mongoDatabase = getMongoDatabase(client, testProperties);
        var testCollection = getTestCollection(mongoDatabase);
        assertThat(testCollection.countDocuments()).isZero();

        insertFirstDocument(testCollection);
        assertThat(testCollection.countDocuments()).isOne();
        assertThat(databaseNames(client)).contains(testProperties.getDatabaseName());
    }

    static void insertFirstDocument(MongoCollection<Document> testCollection) {
        var doc = docToInsertFirst();
        testCollection.insertOne(doc);
    }

    static Document docToInsertFirst() {
        return new Document().append("foo", "bar");
    }

    static void assertNoDocumentsAndInsertSecondDocument(MongoClient client, MongoTestProperties testProperties) {
        var mongoDatabase = getMongoDatabase(client, testProperties);
        var testCollection = getTestCollection(mongoDatabase);
        assertThat(testCollection.countDocuments()).isZero();

        insertSecondDocument(testCollection);
        assertThat(testCollection.countDocuments()).isOne();
        assertThat(databaseNames(client)).contains(testProperties.getDatabaseName());
    }

    static MongoDatabase getMongoDatabase(MongoClient client, MongoTestProperties testProperties) {
        return client.getDatabase(testProperties.getDatabaseName());
    }

    static MongoCollection<Document> getTestCollection(MongoDatabase mongoDatabase) {
        return mongoDatabase.getCollection(TEST_COLLECTION_NAME);
    }

    static void insertSecondDocument(MongoCollection<Document> testCollection) {
        var doc = docToInsertSecond();
        testCollection.insertOne(doc);
    }

    static Document docToInsertSecond() {
        return new Document().append("ham", "eggs");
    }

    static List<Document> findAllDocuments(MongoCollection<Document> collection) {
        return newArrayList(collection.find());
    }

    static Document findFirstDocument(List<Document> documents) {
        return getDocumentWithProperty(documents, "foo");
    }

    static Document findSecondDocument(List<Document> documents) {
        return getDocumentWithProperty(documents, "ham");
    }

    private Document getDocumentWithProperty(List<Document> documents, String property) {
        return documents.stream()
                .filter(document -> document.containsKey(property))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Did not find document with property: " + property));
    }
}
