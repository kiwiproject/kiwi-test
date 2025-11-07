package org.kiwiproject.test.junit.jupiter;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.isNull;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.utility.DockerImageName;

@UtilityClass
@Slf4j
public class MongoTestContainerHelpers {

    public static final String ENV_MONGO_IMAGE_NAME = "MONGO_IMAGE_NAME";

    public static final String MONGO_LATEST_IMAGE_NAME = "mongo:latest";

    public static MongoDBContainer startedMongoDBContainer() {
        var imageName = envMongoImageNameOrLatest();
        return startedMongoDBContainer(imageName);
    }

    public static MongoDBContainer startedMongoDBContainer(String dockerImageName) {
        var container = newMongoDBContainer(dockerImageName);
        container.start();
        checkState(container.isRunning());
        return container;
    }

    public static MongoDBContainer newMongoDBContainer() {
        var imageName = envMongoImageNameOrLatest();
        return newMongoDBContainer(imageName);
    }

    private static String envMongoImageNameOrLatest() {
        var envImageName = System.getenv(ENV_MONGO_IMAGE_NAME);
        return isNull(envImageName) ? MONGO_LATEST_IMAGE_NAME : envImageName;
    }

    @SuppressWarnings("resource")  // because Testcontainers closes it for us
    public static MongoDBContainer newMongoDBContainer(String dockerImageName) {
        LOG.info("Create MongoDBContainer for Docker image name: {}", dockerImageName);
        return new MongoDBContainer(DockerImageName.parse(dockerImageName))
                .waitingFor(new HostPortWaitStrategy());
    }
}
