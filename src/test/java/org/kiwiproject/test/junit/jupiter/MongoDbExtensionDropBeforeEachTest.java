package org.kiwiproject.test.junit.jupiter;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.kiwiproject.test.junit.jupiter.MongoDbTestHelpers.assertNoDocumentsAndInsertFirstDocument;
import static org.kiwiproject.test.junit.jupiter.MongoDbTestHelpers.assertNoDocumentsAndInsertSecondDocument;
import static org.kiwiproject.test.junit.jupiter.MongoDbTestHelpers.assertNoTestDatabaseExists;
import static org.kiwiproject.test.junit.jupiter.MongoDbTestHelpers.assertTestDatabaseExists;
import static org.kiwiproject.test.junit.jupiter.MongoDbTestHelpers.buildMongoTestProperties;
import static org.kiwiproject.test.junit.jupiter.MongoDbTestHelpers.startInMemoryMongoServer;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import de.bwaldvogel.mongo.MongoServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.kiwiproject.test.mongo.MongoTestProperties;

import java.net.InetSocketAddress;

@DisplayName("MongoDbExtension: Drop Database @BeforeEach")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MongoDbExtensionDropBeforeEachTest {

    private static final MongoServer MONGO_SERVER = startInMemoryMongoServer();
    private static final InetSocketAddress INET_SOCKET_ADDRESS = MONGO_SERVER.getLocalAddress();
    private static final MongoTestProperties TEST_PROPERTIES = buildMongoTestProperties(INET_SOCKET_ADDRESS);

    private static MongoClient client;

    @BeforeAll
    static void beforeAll() {
        client = new MongoClient(new ServerAddress(INET_SOCKET_ADDRESS));
    }

    @SuppressWarnings("unused")
    @RegisterExtension
    static final MongoDbExtension MONGO_DB_EXTENSION = MongoDbExtension.builder()
            .props(TEST_PROPERTIES)
            .dropTime(MongoDbExtension.DropTime.BEFORE_EACH)
            .skipDatabaseCleanup(true)
            .build();

    @BeforeEach
    void setUp() {
        // the extension's @BeforeEach runs before ours, so we should *never* see the test database here
        assertNoTestDatabaseExists(client);
    }

    @AfterEach
    void tearDown() {
        // we are dropping *before* tests, so we *should* see the databases after each test
        assertTestDatabaseExists(client, TEST_PROPERTIES);
    }

    @AfterAll
    static void afterAll() {
        // we are dropping *before* tests, so we *should* see the database after all tests
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
    void shouldInsertSomething() {
        assertNoDocumentsAndInsertFirstDocument(client, TEST_PROPERTIES);
    }

    @Test
    @Order(3)
    void shouldInsertSomethingElse() {
        assertNoDocumentsAndInsertSecondDocument(client, TEST_PROPERTIES);
    }
}
