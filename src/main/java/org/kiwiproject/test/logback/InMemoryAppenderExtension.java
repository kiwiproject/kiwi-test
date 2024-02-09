package org.kiwiproject.test.logback;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.LoggerFactory;


/**
 * A JUnit 5 extension that allows testing messages logged using Logback.
 * Uses {@link InMemoryAppender} to store logged messages, so that tests
 * can retrieve and verify them later.
 * <p>
 * Please see the usage requirements in {@link #InMemoryAppenderExtension(Class)}
 * and {@link #InMemoryAppenderExtension(Class, String)}, specifically
 * because this extension requires that a {@link ch.qos.logback.core.Appender}
 * exists at the time tests are executed.
 */
@Beta
public class InMemoryAppenderExtension implements BeforeEachCallback, AfterEachCallback {

    private final Class<?> loggerClass;
    private final String appenderName;

    @Getter
    private String logbackConfigFilePath;

    @Getter
    @Accessors(fluent = true)
    private InMemoryAppender appender;

    /**
     * Create a new instance associated with the given Logback logger class.
     * The appender name must be the <em>simple name</em> of the logger class
     * suffixed with "Appender". So, if the logger class is
     * {@code com.acme.space.modulator.SpaceModulatorServiceTest.class}, then the
     * appender <em>must</em> be named {@code SpaceModulatorServiceTestAppender}.
     * <p>
     * This appender <em>must</em> exist, e.g., in your {@code logback-test.xml}
     * configuration file.
     * <p>
     * For example, for a test named {@code MyAwesomeTest}, the
     * {@code src/test/resources/logback-test.xml} must contain:
     * <pre>
     * &lt;appender name="MyAwesomeTestAppender"
     *     class="org.kiwiproject.test.logback.InMemoryAppender"/&gt;
     *
     * &lt;logger name="com.acme.MyAwesomeTest" level="DEBUG"&gt;
     *     &lt;appender-ref ref="MyAwesomeTestAppender"/&gt;
     * &lt;/logger&gt;
     * </pre>
     * <p>
     * Note also the appender <em>must</em> be an {@link InMemoryAppender}.
     *
     * @param loggerClass the class of the test logger
     */
    public InMemoryAppenderExtension(Class<?> loggerClass) {
        this(loggerClass, loggerClass.getSimpleName() + "Appender");
    }

    /**
     * Create a new instance associated with the given Logback logger class
     * which has an appender of type {@link InMemoryAppender} with the name
     * {@code appenderName}.
     * <p>
     * This appender <em>must</em> exist, e.g., in your {@code logback-test.xml}
     * configuration file.
     * <p>
     * For example, for a test named {@code MyAwesomeTest}, the
     * {@code src/test/resources/logback-test.xml} must contain:
     * <pre>
     * &lt;appender name="MyAppender"
     *     class="org.kiwiproject.test.logback.InMemoryAppender"/&gt;
     *
     * &lt;logger name="com.acme.AnAwesomeTest" level="DEBUG"&gt;
     *     &lt;appender-ref ref="MyAppender"/&gt;
     * &lt;/logger&gt;
     * </pre>
     * <p>
     * You then use {@code "MyAppender"} as the value for the
     * {@code appenderName} argument.
     * <p>
     * Note also the appender <em>must</em> be an {@link InMemoryAppender}.
     *
     * @param loggerClass  the class of the test logger
     * @param appenderName the name of the {@link InMemoryAppender}
     */
    public InMemoryAppenderExtension(Class<?> loggerClass, String appenderName) {
        this.loggerClass = loggerClass;
        this.appenderName = appenderName;
    }

    /**
     * The Logback configuration to use if the logging system needs to be reset.
     * <p>
     * For example:
     * <pre>
     * {@literal @}RegisterExtension
     *  private final InMemoryAppenderExtension inMemoryAppenderExtension =
     *          new InMemoryAppenderExtension(InMemoryAppenderTest.class)
     *                  .withLogbackConfigFilePath("acme-logback-test.xml");
     * </pre>
     *
     * @param logbackConfigFilePath the location of the custom Logback configuration file
     * @return this extension, so this can be chained after the constructor
     * @see <a href="https://github.com/kiwiproject/kiwi-test/issues/457">Tests failing because Logback appenders don't exist (#457)</a>
     */
    public InMemoryAppenderExtension withLogbackConfigFilePath(String logbackConfigFilePath) {
        this.logbackConfigFilePath = logbackConfigFilePath;
        return this;
    }

    /**
     * Exposes the {@link InMemoryAppender} associated with {@code loggerClass}.
     * It can be obtained via the {@link #appender()} method. Usually, tests
     * will store an instance in their own {@link org.junit.jupiter.api.BeforeEach BeforeEach}
     * method. For example:
     * <pre>
     * class SpaceModulatorTest {
     *
     *     {@literal @}RegisterExtension
     *      private final InMemoryAppenderExtension inMemoryAppenderExtension =
     *                 new InMemoryAppenderExtension(SpaceModulatorServiceTest.class);
     *
     *      private InMemoryAppender appender;
     *
     *     {@literal @}BeforeEach
     *      void setUp() {
     *          this.appender = inMemoryAppenderExtension.appender();
     *
     *          // additional set up...
     *      }
     *
     *      // tests...
     *  }
     * </pre>
     *
     * @param context the current extension context; never {@code null}
     */
    @Override
    public void beforeEach(ExtensionContext context) {
        var logbackLogger = (Logger) LoggerFactory.getLogger(loggerClass);
        var rawAppender = getAppender(logbackLogger);

        assertThat(rawAppender)
                .describedAs("Expected an appender named '%s' for logger '%s' of type %s",
                        appenderName, loggerClass.getName(), InMemoryAppender.class.getName())
                .isInstanceOf(InMemoryAppender.class);
        appender = (InMemoryAppender) rawAppender;
    }

    @Nullable
    @VisibleForTesting
    @SuppressWarnings("java:S106")
    Appender<ILoggingEvent> getAppender(Logger logbackLogger) {
        var rawAppender = logbackLogger.getAppender(appenderName);

        if (nonNull(rawAppender)) {
            return rawAppender;
        }

        // Write to stdout since the logging system might be hosed...
        System.out.printf(
            "Appender %s not found on logger %s; attempt fix by resetting logging configuration using config: %s%n",
                appenderName, loggerClass, logbackConfigFilePath);
        System.out.println("You can customize the logging configuration using #withLogbackConfigFilePath");

        // Reset the Logback logging system
        getLogbackTestHelper().resetLogbackWithDefaultOrConfig(logbackConfigFilePath);

        // Try again and return whatever we get. It should not be null after resetting, unless
        // the reset failed, or the appender was not configured correctly.
        return logbackLogger.getAppender(appenderName);
    }

    @VisibleForTesting
    protected LogbackTestHelper getLogbackTestHelper() {
        return new LogbackTestHelper();
    }

    /**
     * Clears all logging events from the {@link InMemoryAppender} to ensure each
     * test starts with an empty appender.
     *
     * @param context the current extension context; never {@code null}
     * @see InMemoryAppender#clearEvents()
     */
    @Override
    public void afterEach(ExtensionContext context) {
        appender.clearEvents();
    }
}
