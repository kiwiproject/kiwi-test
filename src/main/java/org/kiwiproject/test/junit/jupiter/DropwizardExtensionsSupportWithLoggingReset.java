package org.kiwiproject.test.junit.jupiter;

import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A JUnit Jupiter meta-annotation that combines {@link DropwizardExtensionsSupport}
 * and {@link ResetLogbackLoggingExtension} to restore Logback logging after all
 * tests complete.
 * <p>
 * This annotation guarantees that {@link ResetLogbackLoggingExtension}'s
 * {@code afterAll} executes <em>after</em> {@link DropwizardExtensionsSupport}'s
 * {@code afterAll}, so Logback is restored to the default {@code logback-test.xml}
 * configuration once the test class finishes. This prevents Dropwizard's cleanup
 * from leaving later test classes with no log output.
 * <p>
 * <strong>Limitation:</strong> because both extensions are registered via
 * {@code @ExtendWith} at the type level, {@link ResetLogbackLoggingExtension}'s
 * {@code beforeAll} fires <em>before</em> {@link DropwizardExtensionsSupport}
 * bootstraps Dropwizard. Dropwizard then resets Logback again during its own
 * bootstrap, so logging is <em>not</em> restored during the test class itself -
 * only after it completes.
 * <p>
 * If you need logging restored both before and after tests (so that debug/info
 * output is visible during the test run), or if you need a custom Logback
 * configuration instead of the default {@code logback-test.xml}, use the
 * recommended pattern instead: register {@link DropwizardExtensionsSupport}
 * via {@code @ExtendWith} at the class level and
 * {@link ResetLogbackLoggingExtension} as a {@code @RegisterExtension static final}
 * field. See {@link ResetLogbackLoggingExtension} for the recommended pattern
 * and examples.
 * <p>
 * For the afterAll-only use case, this annotation is enough:
 * <pre>
 * {@literal @}DropwizardExtensionsSupportWithLoggingReset
 *  class SomeTest {
 *
 *      // test code that uses DropwizardClientExtension or DropwizardAppExtension
 *  }
 * </pre>
 *
 * @see ResetLogbackLoggingExtension
 * @see DropwizardExtensionsSupport
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(ResetLogbackLoggingExtension.class)
@ExtendWith(DropwizardExtensionsSupport.class)
public @interface DropwizardExtensionsSupportWithLoggingReset {
}
