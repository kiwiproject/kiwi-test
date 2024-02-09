package org.kiwiproject.test.junit.jupiter;

import ch.qos.logback.classic.ClassicConstants;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.kiwiproject.test.logback.LogbackTestHelpers;

/**
 * A JUnit Jupiter {@link org.junit.jupiter.api.extension.Extension Extension} to reset
 * the Logback logging system after all tests have completed.
 * <p>
 * This is useful if something misbehaves, for example Dropwizard's
 * <a href="https://www.dropwizard.io/en/stable/manual/testing.html#integration-testing">DropwizardAppExtension</a> and
 * <a href="https://www.dropwizard.io/en/stable/manual/testing.html#testing-client-implementations">DropwizardClientExtension</a>
 * extensions both stop and detach all appenders after all tests complete! Both of those extensions
 * reset Logback in
 * <a href="https://github.com/dropwizard/dropwizard/blob/297870e3b4b43ea9fb19417dd90ed78151cf6f5d/dropwizard-testing/src/main/java/io/dropwizard/testing/DropwizardTestSupport.java#L244">DropwizardTestSupport</a>.
 * Once this happens, there is no logging output from subsequent tests (since there are no more appenders).
 * We consider this to be <em>bad</em>, since logging output is useful to track down causes if
 * there are other test failures. And, it's just not nice behavior to completely hijack logging!
 * <p>
 * You can use this extension in tests that are using misbehaving components to ensure that Logback
 * is reset after all tests complete, so that subsequent tests have log output.
 * <p>
 * For example to use the default {@code logback-test.xml} as the logging configuration you
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

    @Getter
    @Builder.Default
    private final String logbackConfigFilePath = ClassicConstants.TEST_AUTOCONFIG_FILE;

    @Override
    public void afterAll(ExtensionContext context) {
        LogbackTestHelpers.resetLogback(logbackConfigFilePath);
        LOG.info("Logback was reset using configuration: {}", logbackConfigFilePath);
    }
}
