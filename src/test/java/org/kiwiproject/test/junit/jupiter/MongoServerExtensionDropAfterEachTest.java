package org.kiwiproject.test.junit.jupiter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.kiwiproject.test.junit.jupiter.MongoServerExtensionTestHelpers.assertDocumentHasId;
import static org.kiwiproject.test.junit.jupiter.MongoServerExtensionTestHelpers.getTestCollection;
import static org.kiwiproject.test.junit.jupiter.MongoServerExtensionTestHelpers.insertNewTestDocument;

import com.google.common.collect.ImmutableList;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.kiwiproject.test.junit.jupiter.MongoServerExtension.DropTime;

@DisplayName("MongoServerExtension: Drop Database @AfterEach")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIf(
        value = "org.kiwiproject.test.junit.jupiter.MongoServerExtensionTestHelpers#anyServerVersionSupportsWireVersion8",
        disabledReason = "Must support wire version 8 or higher")
class MongoServerExtensionDropAfterEachTest {

    @RegisterExtension
    static final MongoServerExtension MONGO_SERVER_EXTENSION = new MongoServerExtension(DropTime.AFTER_EACH);

    private MongoCollection<Document> testCollection;

    @BeforeEach
    void setUp() {
        var testDatabase = MONGO_SERVER_EXTENSION.getTestDatabase();
        testCollection = getTestCollection(testDatabase);
    }

    @Test
    @Order(1)
    void shouldInsertNewDocument() {
        var doc = insertNewTestDocument(testCollection);
        assertDocumentHasId(doc);
    }

    @Test
    @Order(2)
    void shouldNotSeePreviousDocument() {
        var documents = ImmutableList.copyOf(testCollection.find());
        assertThat(documents).isEmpty();

        insertNewTestDocument(testCollection);
        insertNewTestDocument(testCollection);
    }

    @Test
    @Order(3)
    void shouldNotSeeAnyPreviousDocuments() {
        var documents = ImmutableList.copyOf(testCollection.find());
        assertThat(documents).isEmpty();
    }
}
