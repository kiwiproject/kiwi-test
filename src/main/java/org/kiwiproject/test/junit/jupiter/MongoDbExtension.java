package org.kiwiproject.test.junit.jupiter;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;
import static org.kiwiproject.base.KiwiStrings.f;
import static org.kiwiproject.test.mongo.MongoTestProperties.UNIT_TEST_ID;
import static org.kiwiproject.test.mongo.MongoTestProperties.UNIT_TEST_ID_SHORT;

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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
 * also tell the extension to skip cleanup of old test databases. You can instantiate this extension using one of the
 * constructors or using the provided builder.
 * <p>
 * For example using a constructor:
 * <pre>
 * private static final MongoTestProperties MONGO_TEST_PROPERTIES = createMongoTestProperties();
 *
 * {@literal @}RegisterExtension
 *  static final MongoDbExtension mongoDbExtension = new MongoDbExtension(MONGO_TEST_PROPERTIES);
 * </pre>
 * Or using a builder:
 * <pre>
 * private static final MongoTestProperties MONGO_TEST_PROPERTIES = createMongoTestProperties();
 *
 * {@literal @}RegisterExtension
 *  static final MongoDbExtension mongoDbExtension = MongoDbExtension.builder()
 *     .props(MONGO TEST PROPERTIES)
 *     .dropTime(DropTime.BEFORE)
 *     .skipCleanup(true)
 *     .build();
 * </pre>
 */
@Slf4j
public class MongoDbExtension implements BeforeEachCallback, AfterEachCallback, AfterAllCallback {

    private static final long CLEANUP_TIME_PERIOD_AMOUNT = 10L;
    private static final ChronoUnit CLEANUP_TIME_PERIOD_UNIT = ChronoUnit.MINUTES;
    private static final String SYSTEM_INDEXES_COLLECTION_NAME = "system.indexes";

    @Getter
    private final DropTime dropTime;

    @Getter
    private final CleanupOption cleanupOption;

    @Getter
    private final boolean skipCleanup;

    @Getter
    private final MongoTestProperties props;

    @Getter
    private final MongoClient mongo;

    @Getter
    private final String mongoUri;

    @Getter
    private final String databaseName;

    /**
     * When to drop the test databases. The extension default is {@link #AFTER_ALL}, which means the database is only
     * dropped after all tests have run.
     */
    public enum DropTime {
        BEFORE_EACH, AFTER_EACH, AFTER_ALL
    }

    /**
     * How to handle records after each individual test. The extension default is {@link #REMOVE_RECORDS}, which will
     * delete the records in existing collections in the test database, but not delete the collections themselves.
     */
    public enum CleanupOption {
        REMOVE_RECORDS, REMOVE_COLLECTION
    }

    /**
     * Create a new extension with the given {@link MongoTestProperties}. The default drop and cleanup options are
     * used. Cleanup of collections is never skipped.
     * <p>
     * Alternatively, use the fluent builder, which also permits changing the {@code skipCleanup} option.
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
     * Alternatively, use the fluent builder, which also permits changing the {@code skipCleanup} option.
     *
     * @param props    the Mongo properties to use
     * @param dropTime when should the test database be dropped?
     */
    public MongoDbExtension(MongoTestProperties props, DropTime dropTime) {
        this(props, dropTime, CleanupOption.REMOVE_RECORDS, false);
    }

    /**
     * Create a new extension with the given {@link MongoTestProperties}, {@link DropTime}, and {@link CleanupOption}.
     * Cleanup of collections is never skipped.
     * <p>
     * Alternatively, use the fluent builder, which also permits changing the {@code skipCleanup} option.
     *
     * @param props         the Mongo properties to use
     * @param dropTime      when should the test database be dropped?
     * @param cleanupOption after each test, should collections be deleted or only the records in the collections?
     */
    public MongoDbExtension(MongoTestProperties props, DropTime dropTime, CleanupOption cleanupOption) {
        this(props, dropTime, cleanupOption, false);
    }

    @Builder
    private MongoDbExtension(MongoTestProperties props,
                             DropTime dropTime,
                             CleanupOption cleanupOption,
                             boolean skipCleanup) {
        this.dropTime = isNull(dropTime) ? DropTime.AFTER_ALL : dropTime;
        this.cleanupOption = isNull(cleanupOption) ? CleanupOption.REMOVE_RECORDS : cleanupOption;
        this.skipCleanup = skipCleanup;
        this.props = props;
        this.mongo = props.newMongoClient();
        this.databaseName = props.getDatabaseName();
        this.mongoUri = props.getUri();
        cleanupDatabasesFromPriorTestRunsIfNecessary();
    }

    private void cleanupDatabasesFromPriorTestRunsIfNecessary() {
        if (skipCleanup) {
            LOG.warn("Skipping cleanup of previous test databases");
            return;
        }

        LOG.debug("Clean up databases from prior test runs that are older than {} {}",
                CLEANUP_TIME_PERIOD_AMOUNT, CLEANUP_TIME_PERIOD_UNIT);

        var keepThresholdMillis = Instant.now().minus(CLEANUP_TIME_PERIOD_AMOUNT, CLEANUP_TIME_PERIOD_UNIT).toEpochMilli();
        var databasesToDrop = newArrayList(mongo.listDatabaseNames().iterator())
                .stream()
                .filter(name -> isUnitTestDatabaseForThisService(name, props))
                .filter(name -> databaseIsOlderThanThreshold(name, keepThresholdMillis))
                .collect(toList());

        LOG.info("Removing {} databases from prior test runs: {}", databasesToDrop.size(), databasesToDrop);
        databasesToDrop.forEach(this::cleanThenDropDatabase);
    }

    @VisibleForTesting
    static boolean isUnitTestDatabaseForThisService(String name, MongoTestProperties props) {
        // TODO This does NOT work if the service name is truncated!
        //  Need to change behavior to see if it matches the database name minus the timestamp
        return name.startsWith(f("{}{}", props.getServiceName(), UNIT_TEST_ID))
                || name.startsWith(f("{}{}", props.getServiceName(), UNIT_TEST_ID_SHORT));
    }

    @VisibleForTesting
    static boolean databaseIsOlderThanThreshold(String name, long keepThresholdMillis) {
        var databaseCreatedAtMillis = MongoTestProperties.extractDatabaseTimestamp(name);
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
