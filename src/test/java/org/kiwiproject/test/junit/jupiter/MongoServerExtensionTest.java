package org.kiwiproject.test.junit.jupiter;

import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.client.model.Filters;
import de.bwaldvogel.mongo.ServerVersion;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;
import java.util.Optional;

@DisplayName("MongoServerExtension")
class MongoServerExtensionTest {

    @RegisterExtension
    static final MongoServerExtension MONGO_SERVER_EXTENSION = new MongoServerExtension();

    @Test
    void shouldProvideMongoServer() {
        assertThat(MONGO_SERVER_EXTENSION.getMongoServer()).isNotNull();
    }

    @Test
    void shouldProvideConnectionString() {
        var mongoServer = MONGO_SERVER_EXTENSION.getMongoServer();
        var localAddress = mongoServer.getLocalAddress();

        assertThat(MONGO_SERVER_EXTENSION.getConnectionString())
                .isEqualTo("mongodb://%s:%d", localAddress.getHostName(), localAddress.getPort());
    }

    @Test
    void shouldProvideTestDatabaseName() {
        assertThat(MONGO_SERVER_EXTENSION.getTestDatabaseName()).isNotBlank();
    }

    @Test
    void shouldProvideTestDatabase() {
        assertThat(MONGO_SERVER_EXTENSION.getTestDatabase()).isNotNull();
    }

    @Test
    void shouldProvideTestDatabaseName_MatchingNameOfTestDatabase() {
        var testDatabase = MONGO_SERVER_EXTENSION.getTestDatabase();
        var testDatabaseName = MONGO_SERVER_EXTENSION.getTestDatabaseName();

        assertThat(testDatabase.getName()).isEqualTo(testDatabaseName);
    }

    @Test
    void shouldProvideMongoClient() {
        assertThat(MONGO_SERVER_EXTENSION.getMongoClient()).isNotNull();
    }

    @Test
    void shouldBeAbleToUseMongoClient() {
        var mongoClient = MONGO_SERVER_EXTENSION.getMongoClient();
        var database = mongoClient.getDatabase(MONGO_SERVER_EXTENSION.getTestDatabaseName());

        var collectionName = "testCollection";
        database.createCollection(collectionName);
        var collection = database.getCollection(collectionName);

        var doc = new Document(Map.of("a", 1, "b", 2, "c", 3));
        collection.insertOne(doc);

        var id = doc.get("_id");
        assertThat(id).isNotNull();

        var firstDoc = Optional.ofNullable(collection.find(Filters.eq("a", 1)).first()).orElseThrow();
        assertThat(firstDoc.get("_id")).isEqualTo(id);
    }

    @Test
    void shouldCreateServerExtensionWithMongo3_0() {
        var extension = new MongoServerExtension(ServerVersion.MONGO_3_0);
        assertThat(extension.getServerVersion()).isEqualTo(ServerVersion.MONGO_3_0);
    }

    @Test
    void shouldCreateServerExtensionWithDefaultMongo3_6() {
        var extension = new MongoServerExtension();
        assertThat(extension.getServerVersion()).isEqualTo(ServerVersion.MONGO_3_6);
    }
}
