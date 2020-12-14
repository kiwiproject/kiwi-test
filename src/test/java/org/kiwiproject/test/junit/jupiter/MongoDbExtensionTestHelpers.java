package org.kiwiproject.test.junit.jupiter;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import lombok.experimental.UtilityClass;
import org.bson.Document;
import org.kiwiproject.test.mongo.MongoTestProperties;

import java.net.InetSocketAddress;
import java.util.List;

@UtilityClass
class MongoDbExtensionTestHelpers {

    public static final String TEST_COLLECTION_NAME = "test_collection";

    static MongoServer startInMemoryMongoServer() {
        var mongoServer = new MongoServer(new MemoryBackend());
        mongoServer.bind();
        return mongoServer;
    }

    static MongoTestProperties buildMongoTestProperties(InetSocketAddress serverAddress) {
        return MongoTestProperties.builder()
                .hostName(serverAddress.getHostName())
                .port(serverAddress.getPort())
                .serviceName("test-service")
                .serviceHost("localhost")
                .build();
    }

    static void assertTestDatabaseExists(MongoClient client, MongoTestProperties testProperties) {
        assertThat(databaseNames(client)).containsExactly(testProperties.getDatabaseName());
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
        assertThat(databaseNames(client)).containsExactly(testProperties.getDatabaseName());
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
        assertThat(databaseNames(client)).containsExactly(testProperties.getDatabaseName());
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
