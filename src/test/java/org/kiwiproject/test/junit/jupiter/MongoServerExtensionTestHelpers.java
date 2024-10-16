package org.kiwiproject.test.junit.jupiter;

import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import de.bwaldvogel.mongo.ServerVersion;
import lombok.experimental.UtilityClass;
import org.bson.Document;

import java.time.Instant;
import java.util.Arrays;

@UtilityClass
class MongoServerExtensionTestHelpers {

    static final String TEST_COLLECTION_NAME = "testCollection";

    static boolean anyServerVersionSupportsWireVersion7() {
        return Arrays.stream(ServerVersion.values())
                .anyMatch(serverVersion -> serverVersion.getWireVersion() > 6);
    }

    static MongoCollection<Document> getTestCollection(MongoDatabase testDatabase) {
        return testDatabase.getCollection(TEST_COLLECTION_NAME);
    }

    static Document insertNewTestDocument(MongoCollection<Document> testCollection) {
        var doc = newTestDocument();
        testCollection.insertOne(doc);
        return doc;
    }

    static Document newTestDocument() {
        var millis = System.currentTimeMillis();
        return new Document()
                .append("timeMillis", millis)
                .append("timeWords", Instant.ofEpochMilli(millis).toString());
    }

    static void assertDocumentHasId(Document doc) {
        assertThat(doc.get("_id")).isNotNull();
    }
}
