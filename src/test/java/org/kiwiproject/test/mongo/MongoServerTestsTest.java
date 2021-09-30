package org.kiwiproject.test.mongo;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.ServerVersion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MongoServerTests")
class MongoServerTestsTest {

    private MongoServer mongoServer;

    @BeforeEach
    void setUp() {
        mongoServer = MongoServerTests.startInMemoryMongoServer();
    }

    @AfterEach
    void tearDown() {
        if (nonNull(mongoServer)) {
            mongoServer.shutdownNow();
        }
    }

    @Test
    void shouldStartInMemoryMongoServer() {
        assertThat(mongoServer).isNotNull();
    }

    @Test
    void shouldStartInMemoryMongoServer_WithServerVersion3() {
        var server = MongoServerTests.startInMemoryMongoServer(ServerVersion.MONGO_3_0);
        assertThat(server).isNotNull();
    }

    @Test
    void shouldGetConnectionString() {
        var connectionString = MongoServerTests.getConnectionString(mongoServer);
        var localAddress = mongoServer.getLocalAddress();

        assertThat(connectionString)
                .isEqualTo("mongodb://%s:%d", localAddress.getHostName(), localAddress.getPort());
    }
}
