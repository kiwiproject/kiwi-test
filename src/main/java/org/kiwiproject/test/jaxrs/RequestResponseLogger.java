package org.kiwiproject.test.jaxrs;

import jakarta.ws.rs.client.Client;
import lombok.experimental.UtilityClass;
import org.glassfish.jersey.logging.LoggingFeature;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This utility class is useful when you are testing REST calls through a Jersey {@link Client} and you want
 * to see the actual HTTP request or response details via the console.
 * <p>
 * The Jersey client API does not give you an easy way to see all of the headers, full URL or request body.
 * This logging will also print out the raw response without having to read the entity.
 * <p>
 * Jersey's way to do this is by registering a {@link LoggingFeature} with the settings you want (e.g. verbosity,
 * logging level, entity size). Unfortunately, this feature requires a {@code java.util.logging Logger} so we are
 * creating a temporary one just for the feature.
 */
@UtilityClass
public class RequestResponseLogger {

    // Logger name defaults to this class
    private static final String DEFAULT_LOGGER_NAME = RequestResponseLogger.class.getName();

    /**
     * /Limit logging of request or response body to 8kB
     */
    public static final int DEFAULT_MAX_ENTITY_SIZE = 8192;

    /**
     * Turn on request/response logging to the console for the given {@link Client} at INFO level.
     *
     * @param client the Jersey Client
     */
    public static void turnOnRequestResponseLogging(Client client) {
        turnOnRequestResponseLogging(client, Level.INFO, LoggingFeature.Verbosity.PAYLOAD_ANY);
    }

    /**
     * Turn on request/response logging to the console for the given {@link Client} at the given level
     * and verbosity.
     *
     * @param client    the Jersey client
     * @param level     the level to log at
     * @param verbosity how much {@link LoggingFeature.Verbosity}
     */
    public static void turnOnRequestResponseLogging(Client client,
                                                    Level level,
                                                    LoggingFeature.Verbosity verbosity) {
        turnOnRequestResponseLogging(client, level, verbosity, DEFAULT_LOGGER_NAME, new ConsoleHandler(), DEFAULT_MAX_ENTITY_SIZE);
    }

    /**
     * Turn on request/response logging allowing control over the level, verbosity, logger name, handler,
     * and max entity size.
     *
     * @param client         the Jersey Client
     * @param level          the level to log at
     * @param verbosity      how much {@link LoggingFeature.Verbosity}
     * @param loggerName     the name for the logger
     * @param loggingHandler the logging {@link Handler}, e.g. a {@link java.util.logging.MemoryHandler}
     * @param maxEntitySize  the maximum entity size
     */
    public static void turnOnRequestResponseLogging(Client client,
                                                    Level level,
                                                    LoggingFeature.Verbosity verbosity,
                                                    String loggerName,
                                                    Handler loggingHandler,
                                                    int maxEntitySize) {
        var logger = Logger.getLogger(loggerName);
        logger.addHandler(loggingHandler);
        logger.setLevel(Level.ALL);

        client.register(new LoggingFeature(logger, level, verbosity, maxEntitySize));
    }
}
