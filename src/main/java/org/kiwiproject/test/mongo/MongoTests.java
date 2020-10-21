package org.kiwiproject.test.mongo;

import com.google.common.annotations.VisibleForTesting;
import lombok.experimental.UtilityClass;
import org.kiwiproject.net.KiwiInternetAddresses;
import org.kiwiproject.net.KiwiInternetAddresses.SimpleHostInfo;
import org.kiwiproject.test.util.ServiceNames;

import java.net.InetAddress;

/**
 * Utilities for database testing with MongoDB. Intended to be used in conjunction with the
 * {@link org.kiwiproject.test.junit.jupiter.MongoDbExtension}, specifically to supply the
 * {@link MongoTestProperties} instance to the extension.
 * <p>
 * Most of the methods in this class are factory methods that create a {@link MongoTestProperties}.
 * for a given service, i.e. a Dropwizard service that uses MongoDB as its datastore. The factory
 * methods either accept a service name directly, or attempt to resolve the service name from a
 * Maven POM file using {@link ServiceNames}.
 */
@UtilityClass
public class MongoTests {

    /**
     * The default MongoDB port.
     */
    public static final int DEFAULT_MONGO_PORT = 27_017;

    private static final String SERVICE_HOST_NAME = getHostNameMinusDomain();

    /**
     * Create a {@link MongoTestProperties} using the given MongoDB host, the default Mongo port, and
     * resolving the service name from the pom.xml file in the current working directory (".").
     *
     * @param mongoHost the host where the MongoDB instance is located
     * @return a new {@link MongoTestProperties} instance
     */
    @SuppressWarnings("UnusedReturnValue")
    public static MongoTestProperties newMongoTestPropertiesFromPom(String mongoHost) {
        return newMongoTestPropertiesFromPom(mongoHost, ".");
    }

    /**
     * Create a {@link MongoTestProperties} using the given MongoDB host, the default Mongo port, and
     * resolving the service name from the pom.xml file in the given root path.
     *
     * @param mongoHost the host where the MongoDB instance is located
     * @param rootPath  the root path of the project, where the Maven pom.xml is located
     * @return a new {@link MongoTestProperties} instance
     */
    public static MongoTestProperties newMongoTestPropertiesFromPom(String mongoHost, String rootPath) {
        return newMongoTestPropertiesFromPom(mongoHost, DEFAULT_MONGO_PORT, rootPath);
    }

    /**
     * Create a {@link MongoTestProperties} using the given MongoDB host, the default Mongo port, and
     * using the given service name.
     *
     * @param mongoHost   the host where the MongoDB instance is located
     * @param serviceName the service name to use as part of the test database name
     * @return a new {@link MongoTestProperties} instance
     */
    public static MongoTestProperties newMongoTestPropertiesWithServiceName(String mongoHost, String serviceName) {
        return newMongoTestPropertiesWithServiceName(mongoHost, DEFAULT_MONGO_PORT, serviceName);
    }

    /**
     * Create a {@link MongoTestProperties} using the given MongoDB host and port, and
     * using the given service name.
     *
     * @param mongoHost the host where the MongoDB instance is located
     * @param mongoPort the port on which the MongoDB instance is listening
     * @return a new {@link MongoTestProperties} instance
     */
    @SuppressWarnings("UnusedReturnValue")
    public static MongoTestProperties newMongoTestPropertiesFromPom(String mongoHost, int mongoPort) {
        return newMongoTestPropertiesFromPom(mongoHost, mongoPort, ".");
    }

    /**
     * Create a {@link MongoTestProperties} using the given MongoDB host and port, and resolving the
     * service name from the pom.xml file in the given root path.
     *
     * @param mongoHost the host where the MongoDB instance is located
     * @param mongoPort the port on which the MongoDB instance is listening
     * @param rootPath  the root path of the project, where the Maven pom.xml is located
     * @return a new {@link MongoTestProperties} instance
     */
    public static MongoTestProperties newMongoTestPropertiesFromPom(String mongoHost, int mongoPort, String rootPath) {
        var serviceName = ServiceNames.findServiceOrEmulatorNameFromRoot(rootPath);
        return newMongoTestPropertiesWithServiceName(mongoHost, mongoPort, serviceName);
    }

    /**
     * Create a {@link MongoTestProperties} using the given MongoDB host and port, and using the
     * given service name.
     *
     * @param mongoHost   the host where the MongoDB instance is located
     * @param mongoPort   the port on which the MongoDB instance is listening
     * @param serviceName the service name to use as part of the test database name
     * @return a new {@link MongoTestProperties} instance
     */
    public static MongoTestProperties newMongoTestPropertiesWithServiceName(String mongoHost,
                                                                            int mongoPort,
                                                                            String serviceName) {
        return MongoTestProperties.builder()
                .hostName(mongoHost)
                .port(mongoPort)
                .serviceHost(SERVICE_HOST_NAME)
                .serviceName(serviceName)
                .build();
    }

    private static String getHostNameMinusDomain() {
        var simpleHostInfo = KiwiInternetAddresses.getLocalHostInfo()
                .orElseGet(MongoTests::fallbackSimpleHostInfo);

        return getHostNameMinusDomain(simpleHostInfo);
    }

    @VisibleForTesting
    static String getHostNameMinusDomain(SimpleHostInfo simpleHostInfo) {
        // Mongo database names cannot have a dot character...so just return host name only excluding domain
        var hostName = simpleHostInfo.getHostName();
        var indexOfFirstDot = hostName.indexOf('.');
        if (indexOfFirstDot == -1) {
            return hostName;
        }

        return hostName.substring(0, indexOfFirstDot);
    }

    @VisibleForTesting
    static SimpleHostInfo fallbackSimpleHostInfo() {
        return SimpleHostInfo.fromInetAddress(InetAddress.getLoopbackAddress());
    }
}
