package org.kiwiproject.test.junit.jupiter;

import com.google.common.annotations.VisibleForTesting;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.kiwiproject.test.logback.LogbackTestHelper;

/**
 * A JUnit Jupiter {@link org.junit.jupiter.api.extension.Extension Extension} to reset
 * the Logback logging system after all tests have completed.
 * <p>
 * This is useful if something misbehaves, for example, Dropwizard's
 * <a href="https://www.dropwizard.io/en/stable/manual/testing.html#integration-testing">DropwizardAppExtension</a> and
 * <a href="https://www.dropwizard.io/en/stable/manual/testing.html#testing-client-implementations">DropwizardClientExtension</a>
 * extensions both stop and detach all appenders after all tests complete! Both of those extensions
 * reset Logback in
 * <a href="https://github.com/dropwizard/dropwizard/blob/297870e3b4b43ea9fb19417dd90ed78151cf6f5d/dropwizard-testing/src/main/java/io/dropwizard/testing/DropwizardTestSupport.java#L244">DropwizardTestSupport</a>.
 * Once this happens, there is either a minimal configuration that only logs at {@code INFO} and higher levels,
 * or worse, there is <em>no logging output</em> from later tests (in the case where it calls Logback's
 * {@link ch.qos.logback.classic.Logger#detachAndStopAllAppenders() Logger#detachAndStopAllAppenders()}.
 * We consider this to be <em>bad</em>, since logging output is useful to track down causes if there are
 * other test failures. And, it's just not nice behavior to completely hijack logging!
 * <p>
 * You can use this extension in tests that are using misbehaving components to ensure that Logback
 * is reset after all tests complete, so that later tests have log output.
 * <p>
 * <strong>It is <em>very</em> important that this extension is registered <em>before</em> extensions
 * like {@code DropwizardExtensionsSupport} that also manipulate Logback. By registering it
 * before the other extensions, the {@code beforeAll} executes first, but the {@code afterAll}
 * executes <em>last</em>.</strong>
 * <p>
 * By executing this extension's {@code afterAll} last, we can guarantee to reset Logback logging.
 * Otherwise, the reset by this extension will be undone by the reset performed by
 * {@code DropwizardExtensionsSupport} (or similar).
 * <p>
 * For example, to use the default {@code logback-test.xml} as the logging configuration you
 * can use {@code @ExtendWith} on the test class:
 * <pre>
 *  {@literal @}ExtendWith(ResetLogbackLoggingExtension.class)  // register first
 *  {@literal @}ExtendWith(DropwizardExtensionsSupport.class)
 *   class SomeTest {
 *
 *       // test code that uses DropwizardClientExtension or DropwizardAppExtension
 *   }
 * </pre>
 * You can also use the {@link DropwizardExtensionsSupportWithLoggingReset} meta-annotation
 * which guarantees the correct order:
 * <pre>
 *   {@literal @}DropwizardExtensionsSupportWithLoggingReset
 *    class SomeTest {
 *
 *        // test code that uses DropwizardClientExtension or DropwizardAppExtension
 *    }
 * </pre>
 * <p>
 * Alternatively, you can register the extension programmatically to use a custom
 * logging configuration. When doing this, you must register {@code DropwizardExtensionsSupport}
 * and similar extensions programmatically as well to ensure the correct order using
 * JUnit's {@link org.junit.jupiter.api.Order Order} annotation.
 * <pre>
 *   class AnotherTest {
 *
 *      {@literal @}Order(1)
 *      {@literal @}RegisterExtension
 *       static final ResetLogbackLoggingExtension RESET_LOGBACK = ResetLogbackLoggingExtension.builder()
 *               .logbackConfigFilePath("acme-special-logback.xml")
 *               .build();
 *
 *      {@literal @}Order(2)
 *      {@literal @}RegisterExtension
 *       static final DropwizardExtensionsSupport DW_SUPPORT = new DropwizardExtensionsSupport();
 *
 *       // test code that uses DropwizardClientExtension or DropwizardAppExtension
 *   }
 * </pre>
 * You can <em>not</em> register a static {@code ResetLogbackLoggingExtension} programmatically and
 * register {@code DropwizardExtensionsSupport} (or similar) using {@code @ExtendWith} at
 * the class level due to JUnit's ordering rules. See
 * <a href="https://docs.junit.org/current/user-guide/#extensions-registration-programmatic">Programmatic Extension Registration</a>
 * in the JUnit reference manual. To be sure of the extension order when using programmatic
 * registration, use explicit {@code @Order} annotations. It's also more clear and does not require
 * remembering the registration order or the differences between static and instance fields.
 *
 * @see io.dropwizard.testing.junit5.DropwizardExtensionsSupport
 * @see DropwizardExtensionsSupportWithLoggingReset
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Slf4j
@SuppressWarnings("LombokGetterMayBeUsed")
public class ResetLogbackLoggingExtension implements AfterAllCallback {

    /**
     * A custom location for the Logback configuration.
     * <p>
     * If this is not set, then the default Logback configuration files are used
     * in the order {@code logback-test.xml} followed by {@code logback.xml}.
     */
    @Getter
    private String logbackConfigFilePath;

    @Override
    public void afterAll(ExtensionContext context) {
        getLogbackTestHelper().resetLogbackWithDefaultOrConfig(logbackConfigFilePath);
        LOG.debug("Logback was reset using configuration: {} (if null, the Logback defaults are used)",
                logbackConfigFilePath);
    }

    @VisibleForTesting
    protected LogbackTestHelper getLogbackTestHelper() {
        return new LogbackTestHelper();
    }
}
