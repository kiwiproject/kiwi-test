package org.kiwiproject.test.mongo;

import static com.google.common.base.Verify.verify;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.containsAny;
import static org.apache.commons.lang3.StringUtils.replaceChars;
import static org.kiwiproject.base.KiwiStrings.f;
import static org.kiwiproject.base.KiwiStrings.splitOnCommas;

import com.google.common.annotations.VisibleForTesting;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import lombok.Builder;
import lombok.Value;
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

    String hostName;
    HostDomainBehavior hostDomainBehavior;
    int port;
    String serviceName;
    String serviceHost;
    String databaseName;
    String uri;

    /**
     * Should the domain be kept or stripped in service host names?
     */
    public enum HostDomainBehavior {
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
     * @param hostName           the host where MongoDB is located
     * @param hostDomainBehavior how to handle domains in the given {@code hostName}
     *                           (defaults to {@link HostDomainBehavior#STRIP STRIP} if this argument is {@code null})
     * @param port               the port that MongoDB is listening on
     * @param serviceName        the name of the service/application being tested
     * @param serviceHost        the host of the service/application being tested
     */
    @Builder
    public MongoTestProperties(String hostName,
                               @Nullable HostDomainBehavior hostDomainBehavior,
                               int port,
                               String serviceName,
                               String serviceHost) {
        this.hostName = hostName;
        this.hostDomainBehavior = isNull(hostDomainBehavior) ? HostDomainBehavior.STRIP : hostDomainBehavior;
        this.port = port;
        this.serviceName = serviceName;
        this.serviceHost = serviceHost(serviceHost, hostDomainBehavior);
        this.databaseName = unitTestDatabaseName(serviceName, serviceHost);
        this.uri = mongoUri(hostName, port, databaseName);
    }

    private static String serviceHost(String serviceHost, HostDomainBehavior hostDomainBehavior) {
        if (hostDomainBehavior == HostDomainBehavior.KEEP) {
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
        // is 4 characters long, such that the total length is (46 + 4 + 13) = 60
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
        var mongoClientURI = new MongoClientURI(uri);
        return new MongoClient(mongoClientURI);
    }
}
