package org.kiwiproject.test.mongo;

import static org.kiwiproject.base.KiwiStrings.f;

import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.ServerVersion;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import lombok.experimental.UtilityClass;

/**
 * Utilities related to the in-memory {@link MongoServer}.
 */
@UtilityClass
public class MongoServerTests {

    /**
     * Start a new in-memory {@link MongoServer}.
     * <p>
     * Defaults server version to Mongo 3.6
     *
     * @return the started MongoServer instance
     */
    public static MongoServer startInMemoryMongoServer() {
        return startInMemoryMongoServer(ServerVersion.MONGO_3_6);
    }

    /**
     * Start a new in-memory {@link MongoServer} with the given {@link ServerVersion}.
     *
     * @param serverVersion the version of Mongo to use
     * @return the started MongoServer instance
     */
    public static MongoServer startInMemoryMongoServer(ServerVersion serverVersion) {
        var backend = new MemoryBackend().version(serverVersion);

        var mongoServer = new MongoServer(backend);
        mongoServer.bind();
        return mongoServer;
    }

    /**
     * Convenience method to build the Mongo database connection string for the given {@link MongoServer}.
     * <p>
     * Example: {@code mongodb://localhost:45678}
     *
     * @param mongoServer the MongoServer instance, used to obtain the host and port
     * @return the connection string
     */
    public static String getConnectionString(MongoServer mongoServer) {
        var addr = mongoServer.getLocalAddress();
        return f("mongodb://{}:{}", addr.getHostName(), addr.getPort());
    }
}
