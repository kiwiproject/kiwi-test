package org.kiwiproject.test.logback;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import com.google.common.annotations.Beta;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.LoggerFactory;


/**
 * A JUnit 5 extension that allows testing messages logged using Logback.
 * Uses {@link InMemoryAppender} to store logged messages, so that tests
 * can retrieve and verify them later.
 */
@Beta
public class InMemoryAppenderExtension implements BeforeEachCallback, AfterEachCallback {

    private final Class<?> loggerClass;
    private final String appenderName;

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
     *
     * @param loggerClass  the class of the test logger
     * @param appenderName the name of the {@link InMemoryAppender}
     */
    public InMemoryAppenderExtension(Class<?> loggerClass, String appenderName) {
        this.loggerClass = loggerClass;
        this.appenderName = appenderName;
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
        var rawAppender = logbackLogger.getAppender(appenderName);

        assertThat(rawAppender)
                .describedAs("Expected an appender named '%s' for logger '%s' of type %s",
                        appenderName, loggerClass.getName(), InMemoryAppender.class.getName())
                .isInstanceOf(InMemoryAppender.class);
        appender = (InMemoryAppender) rawAppender;
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
