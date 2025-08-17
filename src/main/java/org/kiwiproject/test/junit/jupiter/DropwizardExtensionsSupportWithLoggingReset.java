package org.kiwiproject.test.junit.jupiter;

import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A JUnit Jupiter meta-annotation that combines the functionality of {@link DropwizardExtensionsSupport}
 * and {@link ResetLogbackLoggingExtension}.
 * <p>
 * This annotation lets you use Dropwizard's testing support but ensures that the Logback logging system is
 * reset to the configuration in the default {@code logback-test.xml} file after all tests are completed.
 * <p>
 * Use this annotation on test classes to ensure the correct registration order.
 * The proper ordering ensures that {@link ResetLogbackLoggingExtension}'s {@code afterAll} logic
 * is executed last, guaranteeing that the Logback configuration is reset <em>after</em>
 * {@link DropwizardExtensionsSupport} does its own reset.
 * <p>
 * You can use it like this:
 * <pre>
 * {@literal @}DropwizardExtensionsSupportWithLoggingReset
 *  class SomeTestUsingDropwizardExtensions {
 *
 *      // test code that uses DropwizardClientExtension or DropwizardAppExtension
 *  }
 * </pre>
 * If you need to use a custom Logback configuration instead of the default {@code logback-test.xml},
 * then you'll need to register {@link ResetLogbackLoggingExtension} and {@link DropwizardExtensionsSupport}
 * programmatically and ensure the correct order using
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
