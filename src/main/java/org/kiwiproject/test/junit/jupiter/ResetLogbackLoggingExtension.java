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
 * Once this happens, there is no logging output from later tests (since there are no more appenders).
 * We consider this to be <em>bad</em>, since logging output is useful to track down causes if
 * there are other test failures. And, it's just not nice behavior to completely hijack logging!
 * <p>
 * You can use this extension in tests that are using misbehaving components to ensure that Logback
 * is reset after all tests complete, so that later tests have log output.
 * <p>
 * For example, to use the default {@code logback-test.xml} as the logging configuration you
 * can just use {@code @ExtendWith} on the test class:
 * <pre>
 *  {@literal @}ExtendWith(DropwizardExtensionsSupport.class)
 *  {@literal @}ExtendWith(ResetLogbackLoggingExtension.class)
 *   class CustomClientTest {
 *
 *       // test code that uses DropwizardClientExtension
 *   }
 * </pre>
 * Alternatively, you can register the extension programmatically to use a custom
 * logging configuration:
 * <pre>
 *  {@literal @}ExtendWith(DropwizardExtensionsSupport.class)
 *   class CustomClientTest {
 *
 *      {@literal @}RegisterExtension
 *       static final ResetLogbackLoggingExtension RESET_LOGBACK = ResetLogbackLoggingExtension.builder()
 *               .logbackConfigFilePath("acme-special-logback.xml")
 *               .build();
 *
 *       // test code that uses DropwizardClientExtension
 *   }
 * </pre>
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
