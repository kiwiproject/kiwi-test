package org.kiwiproject.test.junit.jupiter;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import lombok.experimental.UtilityClass;
import org.bson.Document;
import org.kiwiproject.test.mongo.MongoTestProperties;

import java.net.InetSocketAddress;
import java.util.List;

@UtilityClass
class MongoDbTestHelpers {

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
        var database = client.getDatabase(testProperties.getDatabaseName());
        var collectionNameIterator = database
                .listCollectionNames()
                .iterator();

        var collectionNames = newArrayList(collectionNameIterator);

        collectionNames.stream()
                .filter(collectionName -> !"system.indexes" .equals(collectionName))
                .forEach(collection -> assertThat(database.getCollection(collection).countDocuments()).isZero());
    }

    static List<String> databaseNames(MongoClient client) {
        return ImmutableList.copyOf(client.listDatabaseNames());
    }

    static void assertInsertSomething(MongoClient client, MongoTestProperties testProperties) {
        var mongoDatabase = client.getDatabase(testProperties.getDatabaseName());
        MongoCollection<Document> testCollection = mongoDatabase.getCollection("testcollection");
        assertThat(testCollection.countDocuments()).isZero();

        var doc = new Document().append("foo", "bar");
        testCollection.insertOne(doc);
        assertThat(testCollection.countDocuments()).isOne();
        assertThat(databaseNames(client)).containsExactly(testProperties.getDatabaseName());
    }

    static void assertInsertSomethingElse(MongoClient client, MongoTestProperties testProperties) {
        var mongoDatabase = client.getDatabase(testProperties.getDatabaseName());
        MongoCollection<Document> testCollection = mongoDatabase.getCollection("testcollection");
        assertThat(testCollection.countDocuments()).isZero();

        var doc = new Document().append("ham", "eggs");
        testCollection.insertOne(doc);
        assertThat(testCollection.countDocuments()).isOne();
        assertThat(databaseNames(client)).containsExactly(testProperties.getDatabaseName());
    }
}
