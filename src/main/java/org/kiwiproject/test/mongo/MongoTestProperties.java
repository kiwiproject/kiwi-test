package org.kiwiproject.test.mongo;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verify;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.containsAny;
import static org.apache.commons.lang3.StringUtils.replaceChars;
import static org.kiwiproject.base.KiwiPreconditions.checkArgumentNotBlank;
import static org.kiwiproject.base.KiwiPreconditions.checkArgumentNotNull;
import static org.kiwiproject.base.KiwiPreconditions.requireNotBlank;
import static org.kiwiproject.base.KiwiStrings.f;
import static org.kiwiproject.base.KiwiStrings.splitOnCommas;

import com.google.common.annotations.VisibleForTesting;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.math.NumberUtils;
import org.kiwiproject.net.KiwiUrls;

import javax.annotation.Nullable;

/**
 * Simple value class that contains properties related to connecting to Mongo in the context of a service that uses
 * MongoDB for persistence. The constructor and builder accept a service/application name and host, which helps to
 * build easily identifiable and unique database names.
 * <p>
 * For example, if you are testing an "order-service" on your continuous integration server that resides on
 * {@code ci-1.acme.com}, the test database name will include this information which can be useful debugging errors as
 * well as to ensure multiple tests of the same service/application can run concurrently on different computers. The
 * current timestamp is also included in the generated test database names to provide additional uniqueness in addition
 * to the service/application name and host.
 * <p>
 * Intended to be used in conjunction with {@link org.kiwiproject.test.junit.jupiter.MongoDbExtension} though there is
 * no reason it cannot be used standalone to generate database names for test purposes as well as the MongoDB
 * client URI.
 * <p>
 * Per MongoDB <a href="https://docs.mongodb.com/manual/reference/limits/#naming-restrictions">Naming Restrictions</a>,
 * "Database names cannot be empty and must have fewer than 64 characters". Thus the maximum length of database names
 * is 63 characters. The generated test database names include the service/application and host name as well as either
 * the {@link #UNIT_TEST_ID} (by default) or {@link #UNIT_TEST_ID_SHORT} (when the name needs to be truncated). The name
 * always ends with the current time in millis. If the service/application name and host are long and including them
 * in their entirety would cause the generated test name to exceed the Mongo name length limit, then various techniques
 * to shorten the name are tried until the name is 63 characters or less.
 */
@Value
public class MongoTestProperties {

    /**
     * The default ID used in generated database names to indicate the purpose is for testing.
     */
    public static final String UNIT_TEST_ID = "_unit_test_";

    /**
     * The short ID used in generated database names to indicate the purpose is for testing. Used in cases where
     * the service/application name and host are lengthy and cause the default generated name to exceed the Mongo
     * name length limit.
     */
    public static final String UNIT_TEST_ID_SHORT = "_ut_";

    private static final String DEFAULT_DB_NAME_TEMPLATE = "{}" + UNIT_TEST_ID + "{}_{}";
    private static final String DB_NAME_TEMPLATE = "{}" + UNIT_TEST_ID + "{}";
    private static final String DB_SHORT_NAME_TEMPLATE = "{}" + UNIT_TEST_ID_SHORT + "{}";

    // Invalid characters for MongoDB database names (union of invalid characters for Windows and Linux/Unix)
    // Linux/Unix invalid characters: /\. "$
    // Windows invalid characters:    /\. "$*<>:|?
    // See https://docs.mongodb.com/manual/reference/limits/#naming-restrictions
    private static final String INVALID_DB_NAME_CHARS = "/\\. \"$*<>:|?";

    // Minimum expected length for timestamps returned by System.currentTimeMillis()
    private static final int MIN_TIMESTAMP_LENGTH = 13;

    String hostName;
    int port;
    String serviceName;
    String serviceHost;
    ServiceHostDomain serviceHostDomain;
    String databaseName;
    String uri;

    /**
     * Should the domain be kept or stripped in service host names?
     */
    public enum ServiceHostDomain {
        /**
         * Keep the domain name, e.g. {@code service1.acme.com} stays as-is.
         */
        KEEP,

        /**
         * Strip the domain name, e.g. {@code service1.acme.com} and {@code service1.test} both become {@code service1}
         */
        STRIP
    }

    /**
     * Constructs a new instance.
     * <p>
     * Use the fluent builder as an alternative to this constructor.
     *
     * @param hostName          the host where MongoDB is located
     * @param port              the port that MongoDB is listening on
     * @param serviceName       the name of the service/application being tested
     * @param serviceHost       the host of the service/application being tested
     * @param serviceHostDomain how to handle domains in the given {@code serviceHost}
     *                          (defaults to {@link ServiceHostDomain#STRIP STRIP} if this argument is {@code null})
     */
    @Builder
    public MongoTestProperties(String hostName,
                               int port,
                               String serviceName,
                               String serviceHost,
                               @Nullable ServiceHostDomain serviceHostDomain) {

        this.hostName = requireNotBlank(hostName);
        this.port = requireValidPort(port);
        this.serviceName = requireNotBlank(serviceName);

        var nonNullServiceHostDomain = isNull(serviceHostDomain) ? ServiceHostDomain.STRIP : serviceHostDomain;
        this.serviceHostDomain = nonNullServiceHostDomain;

        var normalizedServiceHost = serviceHost(requireNotBlank(serviceHost), nonNullServiceHostDomain);
        this.serviceHost = normalizedServiceHost;

        this.databaseName = unitTestDatabaseName(serviceName, normalizedServiceHost);
        this.uri = mongoUri(hostName, port, databaseName);
    }

    private static int requireValidPort(int port) {
        checkArgument(port >= 0 && port <= 65_535, "invalid port: must be in range [0, 65535]");
        return port;
    }

    private static String serviceHost(String serviceHost, ServiceHostDomain serviceHostDomain) {
        checkArgumentNotNull(serviceHostDomain);

        if (serviceHostDomain == ServiceHostDomain.KEEP) {
            return serviceHost;
        }

        return KiwiUrls.extractSubDomainNameFrom(serviceHost).orElseThrow();
    }

    @VisibleForTesting
    static String unitTestDatabaseName(String serviceName, String serviceHost) {
        var now = System.currentTimeMillis();
        var dbName = f(DEFAULT_DB_NAME_TEMPLATE, serviceName, serviceHost, now);

        // If name exceeds the limit of 63 characters, try removing the subdomain from hostname
        if (nameExceedsMongoLength(dbName)) {
            var subDomain = KiwiUrls.extractSubDomainNameFrom(serviceHost).orElse("");
            dbName = f(DEFAULT_DB_NAME_TEMPLATE, serviceName, subDomain, now);
        }

        // If name still exceeds the limit, try removing the entire hostname
        if (nameExceedsMongoLength(dbName)) {
            dbName = f(DB_NAME_TEMPLATE, serviceName, now);
        }

        // If name still exceeds limit, try using the short ID
        if (nameExceedsMongoLength(dbName)) {
            dbName = f(DB_SHORT_NAME_TEMPLATE, serviceName, now);
        }

        // If name still exceeds limit, chop service name down to 46 characters. This also assumes the short test ID
        // is 4 characters long, such that the total length is (46 + 4 + 13) = 63
        // NOTE: 46 characters is accurate only so long as System.currentTimeMillis() returns a number 13 digits long,
        // which means we're good through Sat Nov 20 2286 17:46:39 GMT+0000 (GMT)
        if (nameExceedsMongoLength(dbName)) {
            dbName = f(DB_SHORT_NAME_TEMPLATE, serviceName.substring(0, 46), now);
        }

        dbName = replaceInvalidDatabaseNameCharactersIfPresent(dbName);

        verifyDatabaseNameLength(dbName);

        return dbName;
    }

    private static String replaceInvalidDatabaseNameCharactersIfPresent(String dbName) {
        if (containsAny(dbName, INVALID_DB_NAME_CHARS)) {
            // Replace any invalid character with an underscore
            return replaceChars(dbName, INVALID_DB_NAME_CHARS, "____________");
        }

        return dbName;
    }

    @VisibleForTesting
    static void verifyDatabaseNameLength(String dbName) {
        verify(!nameExceedsMongoLength(dbName),
                "Unexpected error: DB name must be less than 64 characters in length, but was %s: %s",
                dbName.length(), dbName);
    }

    private static boolean nameExceedsMongoLength(String dbName) {
        return dbName.length() >= 64;
    }

    private static String mongoUri(String dbHostName, int dbPort, String databaseName) {
        var hostAndPort = splitOnCommas(dbHostName)
                .stream()
                .map(hostname -> f("{}:{}", hostname, dbPort))
                .collect(joining(","));
        var optionalReplicaSet = dbHostName.contains(",") ? "?replicaSet=rs0" : "";

        return f("mongodb://{}/{}{}",
                hostAndPort,
                databaseName,
                optionalReplicaSet);
    }

    /**
     * Create a new Mongo client for the test database described by the properties in this instance.
     *
     * @return a new {@link MongoClient}
     */
    public MongoClient newMongoClient() {
        return MongoClients.create(uri);
    }

    /**
     * Get the database name without the trailing underscore plus timestamp.
     * <p>
     * Example: If the database name is {@code test-service_unit_test_host1_1602375491864}, then this method
     * returns {@code test-service_unit_test_host1}.
     *
     * @return the database name without the timestamp
     */
    public String getDatabaseNameWithoutTimestamp() {
        return databaseNameWithoutTimestamp(databaseName);
    }

    /**
     * Do a basic check whether the given database name contains either {@link #UNIT_TEST_ID} or
     * {@link #UNIT_TEST_ID_SHORT}, which is a pretty good indicator that the database name was created
     * by this class. If the name contains either of those, then also check for a timestamp as the last
     * part in the name following the last underscore, i.e. find the last underscore and check if the remainder
     * of the name contains only digits.
     *
     * @param databaseName the database name to check
     * @return true if the given database name looks like a name that was created by this class
     */
    public static boolean looksLikeTestDatabaseName(String databaseName) {
        var containsUnitTestInName = databaseName.contains(UNIT_TEST_ID) || databaseName.contains(UNIT_TEST_ID_SHORT);

        if (!containsUnitTestInName) {
            return false;
        }

        var index = databaseName.lastIndexOf('_');
        verify(index >= 0,
                "database name should have two or more underscores unless UNIT_TEST_ID or UNIT_TEST_ID_SHORT were changed!");
        var lastNameSegment = databaseName.substring(index + 1);

        // Check the last segment is all digits and its length is at least 13 digits, since System.currentTimeMillis
        // currently returns a 13 digit number, and will continue to do so until the year 2286. After that it will
        // be 14 digits long!

        return lastNameSegment.length() >= MIN_TIMESTAMP_LENGTH && NumberUtils.isDigits(lastNameSegment);
    }

    /**
     * Static utility to get the database name without the timestamp. Performs minimal error checking on the
     * database name. Expects the timestamp to be immediately after the last underscore.
     * <p>
     * Example: If the database name is {@code test-service_unit_test_host1_1602375491864}, then this method
     * returns {@code test-service_unit_test_host1}.
     *
     * @param databaseName the database name
     * @return the database name without the timestamp
     * @throws IllegalArgumentException if the databaseName is blank or does not have any underscores
     */
    public static String databaseNameWithoutTimestamp(String databaseName) {
        var lastUnderscoreIndex = getLastUnderscoreIndex(databaseName);
        return databaseName.substring(0, lastUnderscoreIndex);
    }

    /**
     * Get the database timestamp.
     * <p>
     * Example: If the database name is {@code test-service_unit_test_host1_1602375491864}, then this method
     * returns {@code 1602375491864}.
     *
     * @return the timestamp
     */
    public long getDatabaseTimestamp() {
        return extractDatabaseTimestamp(databaseName);
    }

    /**
     * Static utility to extract the database timestamp from the given database name. Performs minimal error checking
     * on the database name. Expects the timestamp to be immediately after the last underscore.
     * <p>
     * Example: If the database name is {@code test-service_unit_test_host1_1602375491864}, then this method
     * returns {@code 1602375491864}.
     *
     * @param databaseName the database name
     * @return the timestamp
     * @throws IllegalArgumentException if the databaseName is blank or does not have any underscores
     * @throws NumberFormatException    if the extracted "timestamp" cannot be parsed into a {@code long}
     */
    public static long extractDatabaseTimestamp(String databaseName) {
        var lastUnderscoreIndex = getLastUnderscoreIndex(databaseName);
        return Long.parseLong(databaseName.substring(lastUnderscoreIndex + 1));
    }

    private static int getLastUnderscoreIndex(String databaseName) {
        checkArgumentNotBlank(databaseName, "databaseName cannot be blank");
        checkArgument(!databaseName.endsWith("_"), "databaseName cannot end with an underscore");
        var lastUnderscoreIndex = databaseName.lastIndexOf('_');
        checkArgument(lastUnderscoreIndex > -1, "databaseName does not have correct format");
        return lastUnderscoreIndex;
    }
}
