package org.kiwiproject.test.junit.jupiter;

import com.google.common.annotations.VisibleForTesting;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.kiwiproject.test.logback.LogbackTestHelper;

/**
 * A JUnit Jupiter {@link org.junit.jupiter.api.extension.Extension Extension} that resets
 * the Logback logging system both before and after all tests in a class have run.
 * <p>
 * This is useful when something misbehaves with Logback. For example, Dropwizard's
 * <a href="https://www.dropwizard.io/en/stable/manual/testing.html#integration-testing">DropwizardAppExtension</a> and
 * <a href="https://www.dropwizard.io/en/stable/manual/testing.html#testing-client-implementations">DropwizardClientExtension</a>
 * reset Logback during bootstrap (silencing DEBUG and INFO logs during test execution) and again
 * after all tests complete (stopping and detaching all appenders, which can leave later test
 * classes with no log output at all). Both of those extensions reset Logback in
 * <a href="https://github.com/dropwizard/dropwizard/blob/297870e3b4b43ea9fb19417dd90ed78151cf6f5d/dropwizard-testing/src/main/java/io/dropwizard/testing/DropwizardTestSupport.java#L244">DropwizardTestSupport</a>.
 * Once this happens, there is either a minimal configuration that only logs at {@code INFO} and higher levels,
 * or worse, there is <em>no logging output</em> from later tests (in the case where it calls Logback's
 * {@link ch.qos.logback.classic.Logger#detachAndStopAllAppenders() Logger#detachAndStopAllAppenders()}).
 * We consider this to be <em>bad</em>, since logging output is useful to track down causes if there are
 * other test failures. And, it's just not nice behavior to completely hijack logging!
 *
 * <h2>Recommended usage (as of kiwi-test 4.2.0)</h2>
 * <p>
 * Declare this extension as a {@code static final} field with
 * {@link org.junit.jupiter.api.extension.RegisterExtension @RegisterExtension}, while keeping
 * {@code DropwizardExtensionsSupport} registered via {@code @ExtendWith} at the class level.
 * Because {@code @ExtendWith} extensions are "outer" relative to {@code @RegisterExtension} field
 * extensions, JUnit guarantees that {@code DropwizardExtensionsSupport}'s {@code beforeAll} always
 * runs before this extension's {@code beforeAll}, and this extension's {@code afterAll} always runs
 * before {@code DropwizardExtensionsSupport}'s {@code afterAll}. This means both the
 * before-test and after-test Logback resets work correctly without any {@code @Order} annotations.
 * <p>
 * To use the default {@code logback-test.xml} configuration:
 * <pre>
 *  {@literal @}ExtendWith(DropwizardExtensionsSupport.class)
 *   class SomeTest {
 *
 *      {@literal @}RegisterExtension
 *       static final ResetLogbackLoggingExtension RESET_LOGBACK = new ResetLogbackLoggingExtension();
 *
 *       // test code that uses DropwizardAppExtension or DropwizardClientExtension
 *   }
 * </pre>
 * To use a custom Logback configuration:
 * <pre>
 *  {@literal @}ExtendWith(DropwizardExtensionsSupport.class)
 *   class SomeTest {
 *
 *      {@literal @}RegisterExtension
 *       static final ResetLogbackLoggingExtension RESET_LOGBACK = ResetLogbackLoggingExtension.builder()
 *               .logbackConfigFilePath("acme-special-logback.xml")
 *               .build();
 *
 *       // test code that uses DropwizardAppExtension or DropwizardClientExtension
 *   }
 * </pre>
 *
 * <h2>Using {@code @ExtendWith} for both extensions</h2>
 * <p>
 * If you only need the {@code afterAll} reset (i.e. you do not need logging restored before tests
 * run), you can still use {@code @ExtendWith} for both extensions. However, the ordering is
 * critical: {@code ResetLogbackLoggingExtension} <em>must</em> be declared first so that its
 * {@code afterAll} executes last (after Dropwizard has finished resetting Logback).
 * The {@code beforeAll} in this case fires before Dropwizard's bootstrap reset, so it is a
 * harmless no-op rather than a fix for in-test logging.
 * <pre>
 *  {@literal @}ExtendWith(ResetLogbackLoggingExtension.class)  // must be first
 *  {@literal @}ExtendWith(DropwizardExtensionsSupport.class)
 *   class SomeTest {
 *
 *       // test code that uses DropwizardAppExtension or DropwizardClientExtension
 *   }
 * </pre>
 * You can also use the {@link DropwizardExtensionsSupportWithLoggingReset} meta-annotation,
 * which guarantees the correct order:
 * <pre>
 *  {@literal @}DropwizardExtensionsSupportWithLoggingReset
 *   class SomeTest {
 *
 *       // test code that uses DropwizardAppExtension or DropwizardClientExtension
 *   }
 * </pre>
 *
 * <h2>Both extensions as {@code @RegisterExtension} fields</h2>
 * <p>
 * If {@code DropwizardExtensionsSupport} is also registered as a {@code @RegisterExtension} field
 * rather than via {@code @ExtendWith}, there is no {@code @Order} combination that satisfies both
 * the {@code beforeAll} and {@code afterAll} ordering requirements simultaneously. Migrate to the
 * recommended pattern above ({@code @ExtendWith(DropwizardExtensionsSupport.class)} at the class
 * level) to get the full benefit of both callbacks.
 *
 * <h2>Without Dropwizard</h2>
 * <p>
 * If you are not using Dropwizard, {@code @ExtendWith(ResetLogbackLoggingExtension.class)} is
 * sufficient. The {@code beforeAll} fires harmlessly, and {@code afterAll} resets Logback after
 * all tests complete.
 *
 * @see io.dropwizard.testing.junit5.DropwizardExtensionsSupport
 * @see DropwizardExtensionsSupportWithLoggingReset
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Slf4j
@SuppressWarnings("LombokGetterMayBeUsed")
public class ResetLogbackLoggingExtension implements BeforeAllCallback, AfterAllCallback {

    /**
     * A custom location for the Logback configuration.
     * <p>
     * If this is not set, then the default Logback configuration files are used
     * in the order {@code logback-test.xml} followed by {@code logback.xml}.
     */
    @Getter
    private String logbackConfigFilePath;

    @Override
    public void beforeAll(ExtensionContext context) {
        getLogbackTestHelper().resetLogbackWithDefaultOrConfig(logbackConfigFilePath);
        LOG.debug("Logback was reset (before all tests) using configuration: {} (if null, the Logback defaults are used)",
                logbackConfigFilePath);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        getLogbackTestHelper().resetLogbackWithDefaultOrConfig(logbackConfigFilePath);
        LOG.debug("Logback was reset (after all tests) using configuration: {} (if null, the Logback defaults are used)",
                logbackConfigFilePath);
    }

    @VisibleForTesting
    protected LogbackTestHelper getLogbackTestHelper() {
        return new LogbackTestHelper();
    }
}
