package org.kiwiproject.test.junit.jupiter;

import static org.kiwiproject.base.KiwiPreconditions.requireNotNull;
import static org.kiwiproject.base.KiwiStrings.f;
import static org.kiwiproject.test.mongo.MongoServerTests.startInMemoryMongoServer;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.ServerVersion;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.kiwiproject.test.mongo.MongoServerTests;

import java.util.concurrent.ThreadLocalRandom;

/**
 * A JUnit Jupiter {@link org.junit.jupiter.api.extension.Extension Extension} that starts an in-memory
 * {@link MongoServer} once before all tests have run, and which shuts it down once after all tests have run.
 * An alternative for testing against "real" MongoDB instances is the {@link MongoDbExtension}.
 * <p>
 * By default, the in-memory test database will be dropped and re-created after each test so that each individual
 * test can start with an empty database. If you need to retain the database between tests, construct an instance
 * with the {@link DropTime#AFTER_ALL} option. The database will not be dropped or modified in any way before or
 * after tests when this option is used. This is most useful in integration test scenarios in which tests are executed
 * in a specific order and build upon the results of previous tests.
 * <p>
 * This extension also creates a test database with a unique name that can be used in tests, and provides accessors
 * to enable tests to obtain various objects such as the connection string, the test database name, a
 * {@link MongoDatabase} for the test database, a {@link MongoClient}, and more.
 * <p>
 * By default, the in-memory Mongo server will be set to the 3.6 flavor of Mongo.  If you need to use the 3.0 version of
 * Mongo, then the {@link ServerVersion} can be passed into the constructor to set the version.
 * <p>
 * Usage:
 * <pre>
 * class MyMongoTest {
 *
 *    {@literal @}RegisterExtension
 *     static final MongoServerExtension MONGO_SERVER_EXTENSION =
 *             new MongoServerExtension();
 *
 *    {@literal @}Test
 *     void testInsert() {
 *         var database = MONGO_SERVER_EXTENSION.getTestDatabase();
 *         database.createCollection("my_collection");
 *         // ...
 *     }
 * }
 * </pre>
 *
 * @see MongoDbExtension
 */
@Slf4j
public class MongoServerExtension implements BeforeAllCallback, AfterAllCallback, AfterEachCallback {

    private static final ServerVersion DEFAULT_SERVER_VERSION = ServerVersion.MONGO_3_6;

    /**
     * The in-memory {@link MongoServer} instance started by this extension.
     */
    @Getter
    private MongoServer mongoServer;

    /**
     * Returns the Mongo connection string for the started {@link MongoServer}, e.g. {@code mongodb://localhost:34567}.
     * It does <em>not</em> include the test database name, since this extension does not preclude creating other
     * databases, for example a scenario in which the class under test integrates data from multiple MongoDB
     * databases.
     */
    @Getter
    private String connectionString;

    /**
     * The name of the test database created by this extension.
     * <p>
     * This is same across tests. Even though (by default using {@link DropTime#AFTER_EACH AFTER_EACH}) the test
     * database is dropped after each test, a fresh database with the same name is immediately re-created following
     * the drop.
     */
    @Getter
    private String testDatabaseName;

    /**
     * The test {@link MongoDatabase} created by this extension.
     * <p>
     * When drop time is {@link DropTime#AFTER_EACH AFTER_EACH} (the default), a new instance is created after each
     * test. Therefore tests should always re-initialize the database, for example in a {@code @BeforeEach} method.
     */
    @Getter
    private MongoDatabase testDatabase;

    /**
     * A {@link MongoClient} that can be used in tests. This should never be closed by tests, as this client is
     * opened before all tests, and closed after all tests have executed.
     */
    @Getter
    private MongoClient mongoClient;

    /**
     * When to drop test databases.
     */
    @Getter
    private final DropTime dropTime;

    /**
     * The version of the mongo server to use.
     */
    @Getter
    private final ServerVersion serverVersion;

    /**
     * Creates a new instance that will drop and re-create the test database after each test.
     * <p>
     * The Mongo server will be set to the 3.6 version
     */
    public MongoServerExtension() {
        this(DropTime.AFTER_EACH, DEFAULT_SERVER_VERSION);
    }

    /**
     * Creates a new instance that will drop the test database using the given {@link DropTime}.
     * <p>
     * The Mongo server will be set to the 3.6 version
     *
     * @param dropTime when to drop the test database
     */
    public MongoServerExtension(DropTime dropTime) {
        this(dropTime, DEFAULT_SERVER_VERSION);
    }

    /**
     * Creates a new instance that will drop and re-create the test database after each test with the given {@link ServerVersion}.
     *
     * @param serverVersion the version of the mongo server to use.
     */
    public MongoServerExtension(ServerVersion serverVersion) {
        this(DropTime.AFTER_EACH, serverVersion);
    }

    /**
     * Creates a new instance that will drop the test database using the given {@link DropTime} and uses the given
     * {@link ServerVersion}.
     *
     * @param dropTime when to drop the test database
     * @param serverVersion the version of the mongo server to use
     */
    public MongoServerExtension(DropTime dropTime, ServerVersion serverVersion) {
        this.dropTime = requireNotNull(dropTime, "dropTime cannot be null");
        this.serverVersion = requireNotNull(serverVersion, "serverVersion cannot be null");
    }

    /**
     * When to drop the test databases. The extension default is {@link DropTime#AFTER_EACH}, so that each test has
     * a fresh database to test against. You can also drop the database after all tests have been run, which can be
     * useful in certain integration test scenarios with ordered tests, for example when each test is a part of an
     * overall workflow.
     * <p>
     * Note that because this extension uses an in-memory server, dropping and re-creating the test database after each
     * test is no different than dropping before each test, so only the after each option is provided.
     */
    public enum DropTime {
        AFTER_EACH, AFTER_ALL
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        mongoServer = startInMemoryMongoServer(serverVersion);
        connectionString = MongoServerTests.getConnectionString(mongoServer);
        testDatabaseName = generateTestDatabaseName();

        mongoClient = new MongoClient(new MongoClientURI(connectionString));
        testDatabase = mongoClient.getDatabase(testDatabaseName);
    }

    private static String generateTestDatabaseName() {
        var millis = System.currentTimeMillis();
        var random = ThreadLocalRandom.current().nextInt(10_000);
        return f("test_db_{}_{}", millis, random);
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        if (dropTime == DropTime.AFTER_EACH) {
            dropAndRecreateTestDatabase();
            LOG.trace("@AfterEach: Database {} was dropped and re-created", testDatabaseName);
        }
    }

    private void dropAndRecreateTestDatabase() {
        mongoClient.dropDatabase(testDatabaseName);
        this.testDatabase = mongoClient.getDatabase(testDatabaseName);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        LOG.trace("@AfterAll: Closing Mongo client and shutting in-memory MongoServer down");
        mongoClient.close();
        mongoServer.shutdownNow();
    }
}
