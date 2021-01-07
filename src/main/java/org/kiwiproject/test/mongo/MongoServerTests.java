package org.kiwiproject.test.mongo;

import static org.kiwiproject.base.KiwiStrings.f;

import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import lombok.experimental.UtilityClass;

/**
 * Utilities related to the in-memory {@link MongoServer}.
 */
@UtilityClass
public class MongoServerTests {

    /**
     * Start a new in-memory {@link MongoServer}.
     *
     * @return the started MongoServer instance
     */
    public static MongoServer startInMemoryMongoServer() {
        var mongoServer = new MongoServer(new MemoryBackend());
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
