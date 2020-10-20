package org.kiwiproject.test.mongo;

import com.google.common.annotations.VisibleForTesting;
import lombok.experimental.UtilityClass;
import org.kiwiproject.net.KiwiInternetAddresses;
import org.kiwiproject.net.KiwiInternetAddresses.SimpleHostInfo;
import org.kiwiproject.test.util.ServiceNames;

import java.net.InetAddress;

@UtilityClass
public class MongoTests {

    public static final int DEFAULT_MONGO_PORT = 27_017;

    private static final String SERVICE_HOST_NAME = getHostNameMinusDomain();

    @SuppressWarnings("UnusedReturnValue")
    public static MongoTestProperties newMongoTestPropertiesFromPom(String mongoHost) {
        return newMongoTestPropertiesFromPom(mongoHost, ".");
    }

    public static MongoTestProperties newMongoTestPropertiesFromPom(String mongoHost, String rootPath) {
        return newMongoTestPropertiesFromPom(mongoHost, DEFAULT_MONGO_PORT, rootPath);
    }

    public static MongoTestProperties newMongoTestPropertiesWithServiceName(String mongoHost, String serviceName) {
        return newMongoTestPropertiesWithServiceName(mongoHost, DEFAULT_MONGO_PORT, serviceName);
    }

    @SuppressWarnings("UnusedReturnValue")
    public static MongoTestProperties newMongoTestPropertiesFromPom(String mongoHost, int mongoPort) {
        return newMongoTestPropertiesFromPom(mongoHost, mongoPort, ".");
    }

    public static MongoTestProperties newMongoTestPropertiesFromPom(String mongoHost, int mongoPort, String rootPath) {
        var serviceName = ServiceNames.findServiceOrEmulatorNameFromRoot(rootPath);
        return newMongoTestPropertiesWithServiceName(mongoHost, mongoPort, serviceName);
    }

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
        var simpleHostInfo = KiwiInternetAddresses.getLocalHostInfo().orElseGet(MongoTests::fallbackSimpleHostInfo);

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
