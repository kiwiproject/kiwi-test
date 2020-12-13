package org.kiwiproject.test.junit.jupiter;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;
import static org.kiwiproject.base.KiwiPreconditions.requireNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.kiwiproject.test.mongo.MongoTestProperties;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A JUnit Jupiter {@link org.junit.jupiter.api.extension.Extension Extension} to use in Mongo unit/integration tests.
 * This extension must be registered at the class level since it implements {@link AfterAllCallback}, i.e. the field
 * should be declared as {@code static}.
 * <p>
 * This extension by default drops the test Mongo database after <em>all</em> tests have run but clears records from
 * collections after <em>each</em> test runs. The extension also by default cleans up (i.e. deletes) any test databases
 * older than 10 minutes when it runs.
 * <p>
 * You must supply a {@link org.kiwiproject.test.mongo.MongoTestProperties} instance which tells the extension which
 * Mongo database to use by registering the extension (see below).
 * <p>
 * You can control when the database is dropped using the {@link DropTime} argument. You can control whether collections
 * are dropped after each test or whether only the collection records are deleted using {@link CleanupOption}. You can
 * also tell the extension to skip cleanup of old test databases using the {@code skipDatabaseCleanup} (only available
 * via the builder). Finally, you can instantiate this extension using one of the constructors or using the provided
 * builder.
 * <p>
 * Note also that if using an in-memory Mongo server (e.g.
 * <a href="https://mvnrepository.com/artifact/de.bwaldvogel/mongo-java-server">mongo-java-server</a>), then the
 * {@link DropTime} and {@link CleanupOption} only make sense during the execution of a single test class. For example
 * given a {@code SomethingUsingMongoTest} class containing a bunch of individual tests, these options can still be
 * used, but once JUnit has executed all the tests in the class, obviously the database and collections are destroyed
 * when the in-memory server shuts down. An alternative to this extension for using an in-memory MongoDB is the
 * {@link MongoServerExtension}.
 * <p>
 * For example using a constructor:
 * <pre>
 *  private static final MongoTestProperties MONGO_TEST_PROPERTIES = createMongoTestProperties();
 *
 * {@literal @}RegisterExtension
 *  static final MongoDbExtension mongoDbExtension = new MongoDbExtension(MONGO_TEST_PROPERTIES);
 * </pre>
 * Or using a builder:
 * <pre>
 *  private static final MongoTestProperties MONGO_TEST_PROPERTIES = createMongoTestProperties();
 *
 * {@literal @}RegisterExtension
 *  static final MongoDbExtension mongoDbExtension = MongoDbExtension.builder()
 *     .props(MONGO_TEST_PROPERTIES)
 *     .dropTime(DropTime.BEFORE)
 *     .skipDatabaseCleanup(true)
 *     .build();
 * </pre>
 * Using a builder with all options to never drop test databases during test execution and only
 * cleanup old test databases that are older than 60 minutes:
 * <pre>
 *  private static final MongoTestProperties MONGO_TEST_PROPERTIES = createMongoTestProperties();
 *
 * {@literal @}RegisterExtension
 *  static final MongoDbExtension mongoDbExtension = MongoDbExtension.builder()
 *     .props(MONGO_TEST_PROPERTIES)
 *     .dropTime(DropTime.NEVER)
 *     .cleanupOption(CleanupOption.NEVER)
 *     .skipDatabaseCleanup(false)
 *     .databaseCleanupThreshold(Duration.ofMinutes(60))
 *     .build();
 * </pre>
 *
 * @see MongoServerExtension
 */
@Slf4j
public class MongoDbExtension implements BeforeEachCallback, AfterEachCallback, AfterAllCallback {

    private static final Duration DEFAULT_CLEANUP_THRESHOLD = Duration.of(10L, ChronoUnit.MINUTES);
    private static final String SYSTEM_INDEXES_COLLECTION_NAME = "system.indexes";

    /**
     * When to drop test databases.
     */
    @Getter
    private final DropTime dropTime;

    /**
     * How to cleanup collections in test databases.
     */
    @Getter
    private final CleanupOption cleanupOption;

    /**
     * Should test databases from previous test executions be deleted?
     */
    @Getter
    private final boolean skipDatabaseCleanup;

    /**
     * How old can a test database be before it will be automatically cleaned up (deleted)?
     */
    @Getter
    private final Duration databaseCleanupThreshold;

    /**
     * The connection properties for the MongoDB server to be used for test databases.
     */
    @Getter
    private final MongoTestProperties props;

    /**
     * A {@link MongoClient} that can be used in tests.
     */
    @Getter
    private final MongoClient mongo;

    /**
     * The URI of the test database.
     */
    @Getter
    private final String mongoUri;

    /**
     * The test database name.
     */
    @Getter
    private final String databaseName;

    /**
     * When to drop the test databases. The extension default is {@link #AFTER_ALL}, which means the database is only
     * dropped after all tests have run.
     * <p>
     * The {@link #NEVER} option can be used in conjunction with {@link CleanupOption#REMOVE_NEVER} if you are
     * debugging a problematic test and you want to inspect the database after specific tests execute, or if you
     * are managing the database manually before, during, and/or after test execution.
     */
    public enum DropTime {
        BEFORE_EACH, AFTER_EACH, AFTER_ALL, NEVER
    }

    /**
     * How to handle records after each individual test. The extension default is {@link #REMOVE_RECORDS}, which will
     * delete the records in existing collections in the test database, but not delete the collections themselves.
     * <p>
     * The {@link #REMOVE_NEVER} option can be used if your test requires a specific order, e.g. an
     * end-to-end integration test across multiple components that uses @{@link org.junit.jupiter.api.Order Order}
     * to specify the order in which tests execute and which needs to retain data between tests. It can also be used
     * in conjunction with {@link DropTime#NEVER} to debug problematic tests, so that the database state can be
     * inspected after a single test or multiple tests have executed.
     */
    public enum CleanupOption {
        REMOVE_RECORDS, REMOVE_COLLECTION, REMOVE_NEVER
    }

    /**
     * Create a new extension with the given {@link MongoTestProperties}. The default drop and cleanup options are
     * used. Cleanup of collections is never skipped.
     * <p>
     * Alternatively, use the fluent builder, which also permits changing the {@code skipDatabaseCleanup} and
     * {@code databaseCleanupThreshold} options.
     *
     * @param props the Mongo properties to use
     */
    public MongoDbExtension(MongoTestProperties props) {
        this(props, DropTime.AFTER_ALL);
    }

    /**
     * Create a new extension with the given {@link MongoTestProperties} and {@link DropTime}. The default cleanup
     * option is used. Cleanup of collections is never skipped.
     * <p>
     * Alternatively, use the fluent builder, which also permits changing the {@code skipDatabaseCleanup} and
     * {@code databaseCleanupThreshold} options.
     *
     * @param props    the Mongo properties to use
     * @param dropTime when should the test database be dropped?
     */
    public MongoDbExtension(MongoTestProperties props, DropTime dropTime) {
        this(props, dropTime, CleanupOption.REMOVE_RECORDS, false, DEFAULT_CLEANUP_THRESHOLD);
    }

    /**
     * Create a new extension with the given {@link MongoTestProperties}, {@link DropTime}, and {@link CleanupOption}.
     * Cleanup of collections is never skipped.
     * <p>
     * Alternatively, use the fluent builder, which also permits changing the {@code skipDatabaseCleanup} and
     * {@code databaseCleanupThreshold} options.
     *
     * @param props         the Mongo properties to use
     * @param dropTime      when should the test database be dropped?
     * @param cleanupOption after each test, should collections be deleted or only the records in the collections?
     */
    public MongoDbExtension(MongoTestProperties props, DropTime dropTime, CleanupOption cleanupOption) {
        this(props, dropTime, cleanupOption, false, DEFAULT_CLEANUP_THRESHOLD);
    }

    @Builder
    private MongoDbExtension(MongoTestProperties props,
                             DropTime dropTime,
                             CleanupOption cleanupOption,
                             boolean skipDatabaseCleanup,
                             Duration databaseCleanupThreshold) {

        this.props = requireNotNull(props);
        this.dropTime = isNull(dropTime) ? DropTime.AFTER_ALL : dropTime;
        this.cleanupOption = isNull(cleanupOption) ? CleanupOption.REMOVE_RECORDS : cleanupOption;
        this.skipDatabaseCleanup = skipDatabaseCleanup;
        this.databaseCleanupThreshold =
                isNull(databaseCleanupThreshold) ? DEFAULT_CLEANUP_THRESHOLD : databaseCleanupThreshold;
        this.mongo = props.newMongoClient();
        this.databaseName = props.getDatabaseName();
        this.mongoUri = props.getUri();

        cleanupDatabasesFromPriorTestRunsIfNecessary();
    }

    private void cleanupDatabasesFromPriorTestRunsIfNecessary() {
        if (skipDatabaseCleanup) {
            LOG.warn("Skipping cleanup of previous test databases");
            return;
        }

        LOG.debug("Clean up databases from prior test runs that are older than {} ({} minutes)",
                databaseCleanupThreshold, databaseCleanupThreshold.toMinutes());

        var keepThresholdMillis = Instant.now().minus(databaseCleanupThreshold).toEpochMilli();
        var databaseNames = mongo.listDatabaseNames().iterator();
        var databasesToDrop = newArrayList(databaseNames)
                .stream()
                .filter(name -> isUnitTestDatabaseForThisService(name, props))
                .filter(name -> databaseIsOlderThanThreshold(name, keepThresholdMillis))
                .collect(toList());

        LOG.info("Removing {} databases from prior test runs: {}", databasesToDrop.size(), databasesToDrop);
        databasesToDrop.forEach(this::cleanThenDropDatabase);
    }

    @VisibleForTesting
    static boolean isUnitTestDatabaseForThisService(String databaseName, MongoTestProperties props) {
        if (MongoTestProperties.looksLikeTestDatabaseName(databaseName)) {
            return Objects.equals(
                    MongoTestProperties.databaseNameWithoutTimestamp(databaseName),
                    props.getDatabaseNameWithoutTimestamp());
        }

        return false;
    }

    @VisibleForTesting
    static boolean databaseIsOlderThanThreshold(String databaseName, long keepThresholdMillis) {
        var databaseCreatedAtMillis = MongoTestProperties.extractDatabaseTimestamp(databaseName);
        return databaseCreatedAtMillis <= keepThresholdMillis;
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        if (dropTime == DropTime.BEFORE_EACH) {
            dropDatabase();
            LOG.debug("@BeforeEach: Database {} was dropped", databaseName);
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        if (dropTime == DropTime.AFTER_ALL) {
            clearCollections(databaseName, cleanupOption);
            LOG.debug("@AfterEach: Collections cleaned with option {} in database {} @AfterEach", cleanupOption, databaseName);
        } else if (dropTime == DropTime.AFTER_EACH) {
            dropDatabase();
            LOG.debug("@AfterEach: Database {} was dropped", databaseName);
        }
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) {
        if (dropTime == DropTime.AFTER_ALL) {
            dropDatabase();
            LOG.debug("@AfterAll: Database {} was dropped", databaseName);
        }
    }

    private void dropDatabase() {
        LOG.debug("Drop database: {} (dropTime: {})", databaseName, dropTime);
        cleanThenDropDatabase(databaseName);
    }

    private void cleanThenDropDatabase(String databaseName) {
        LOG.debug("Clearing all collections, then dropping database: {}", databaseName);
        clearCollections(databaseName, CleanupOption.REMOVE_COLLECTION);
        dropDb(databaseName);
    }

    private void clearCollections(String databaseName, CleanupOption option) {
        if (option == CleanupOption.REMOVE_NEVER) {
            LOG.debug("Skipping collection cleanup (REMOVE_NEVER)");
            return;
        }

        LOG.debug("Clearing all collections for database: {}", databaseName);
        var mongoDatabase = mongo.getDatabase(databaseName);

        // truncate collections (to avoid re-creating indexes)
        mongoDatabase.listCollectionNames()
                .forEach((Consumer<String>) collection -> {
                    LOG.debug("Cleaning up collection {}.{} -- using cleanup option: {}",
                            mongoDatabase.getName(), collection, option);

                    if (option == CleanupOption.REMOVE_COLLECTION) {
                        LOG.debug("Drop collection {}.{}", mongoDatabase.getName(), collection);
                        mongoDatabase.getCollection(collection).drop();
                    } else {
                        clearCollectionRecords(mongoDatabase, collection);
                    }
                });
        LOG.debug("Done clearing collections for database: {}", databaseName);
    }

    private void clearCollectionRecords(MongoDatabase db, String collection) {
        if (!SYSTEM_INDEXES_COLLECTION_NAME.equals(collection)) {
            LOG.debug("Delete records in {}.{}", db.getName(), collection);
            db.getCollection(collection).deleteMany(new Document());
        }
    }

    private void dropDb(String databaseName) {
        LOG.debug("Dropping database: {}", databaseName);
        var db = mongo.getDatabase(databaseName);
        db.drop();
    }
}
