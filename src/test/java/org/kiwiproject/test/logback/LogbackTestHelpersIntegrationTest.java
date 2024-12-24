package org.kiwiproject.test.logback;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import ch.qos.logback.classic.Logger;
import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.core.Application;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kiwiproject.test.junit.jupiter.ResetLogbackLoggingExtension;
import org.slf4j.LoggerFactory;

/**
 * This integration test uses DropwizardAppExtension which resets Logback when it
 * starts the test application class. It first verifies that there is no appender
 * for this class' Logger, and then uses {@link LogbackTestHelpers#resetLogback()}
 * to reset Logback to the default logging configuration. Finally, it ensures that
 * the InMemoryAppender was reset and that it receives the expected messages.
 * <p>
 * The tests are designed to execute in a specific order and use Jupiter's
 * method ordering feature. The first test executes after DropwizardAppExtension
 * has reset Logback, so we expect the appender to be null. That test resets
 * Logback, after which all later tests should get a non-null appender.
 * <p>
 * In case of failure, this test uses the ResetLogbackLoggingExtension to restore
 * the Logback logging configuration. However, since that extension simply uses
 * {@link LogbackTestHelpers}, it might not work if there is actually a bug and
 * is therefore a bit circular.
 */
@DisplayName("LogbackTestHelpers (Integration Test)")
@ExtendWith(DropwizardExtensionsSupport.class)
@ExtendWith(ResetLogbackLoggingExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
class LogbackTestHelpersIntegrationTest {

    private static final String APPENDER_NAME = "LogbackTestHelpersIntegrationTestAppender";

    public static class MyConfig extends Configuration {
    }

    public static class MyApp extends Application<MyConfig> {

        @Override
        public void run(MyConfig config, Environment environment) {
            // does nothing at all

            environment.healthChecks().register("noop", new HealthCheck() {
                @Override
                protected Result check() {
                    return Result.healthy("Everything's fine here, how are you?");
                }
            });
        }
    }

    @SuppressWarnings("unused")
    static final DropwizardAppExtension<MyConfig> APP_EXTENSION = new DropwizardAppExtension<>(MyApp.class);

    private Logger logbackLogger;
    private InMemoryAppender appender;

    @BeforeEach
    void setUp() {
        logbackLogger = getLogbackLogger();
        appender = getAppender();

        assertThat(logbackLogger)
                .describedAs("Getting the logger as a Logback Logger should always return the same Logger instance")
                .isSameAs(LOG);
    }

    @AfterEach
    void tearDown() {
        // This must re-fetch the appender, since the instance field can be null (for the first test)
        var freshAppender = getAppender();
        if (nonNull(freshAppender)) {
            freshAppender.clearEvents();
        }
    }

    @Test
    @Order(1)
    void shouldResetDefaultLoggingConfiguration() {
        assertThat(appender)
                .describedAs("appender should be null; DropwizardAppExtension is expected to have reset Logback")
                .isNull();

        assertThatCode(() -> {
            LOG.debug("message 1");
            LOG.info("message 2");
            LOG.warn("message 3");
            LOG.error("message 4");
        })
        .describedAs("We should still be able to log things (they just won't go anywhere)")
        .doesNotThrowAnyException();

        LogbackTestHelpers.resetLogback();

        LOG.trace("message 5");
        LOG.debug("message 6");
        LOG.info("message 7");
        LOG.warn("message 8");
        LOG.error("message 9");

        var resetAppender = getAppender();
        assertThat(resetAppender).isNotNull();

        assertThat(resetAppender.orderedEventMessages()).containsExactly(
            "message 6","message 7","message 8", "message 9");
    }

    @Test
    @Order(2)
    void shouldHaveAppenderOnceReset() {
        assertThat(appender)
                .describedAs("appender should not be null; previous test should have reset it")
                .isNotNull();

        LOG.trace("message 0");
        LOG.debug("message A");
        LOG.info("message B");
        LOG.warn("message C");

        assertThat(appender.orderedEventMessages()).containsExactly(
            "message A", "message B", "message C");
    }

    @Test
    @Order(3)
    void shouldStillHaveAppender() {
        assertThat(appender)
                .describedAs("appender should not be null; first test should have reset it")
                .isNotNull();

        LOG.debug("message Z");

        assertThat(appender.orderedEventMessages()).containsExactly("message Z");
    }

    private static Logger getLogbackLogger() {
        return (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(LogbackTestHelpersIntegrationTest.class);
    }

    private InMemoryAppender getAppender() {
        return (InMemoryAppender) logbackLogger.getAppender(APPENDER_NAME);
    }
}
