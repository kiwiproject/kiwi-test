package org.kiwiproject.test.dropwizard.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.kiwiproject.collect.KiwiLists.first;
import static org.kiwiproject.collect.KiwiLists.hasOneElement;
import static org.kiwiproject.test.assertj.KiwiAssertJ.assertIsExactType;
import static org.kiwiproject.test.jaxrs.JaxrsTestHelper.assertOkResponse;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kiwiproject.test.junit.jupiter.ClearBoxTest;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

@DisplayName("DropwizardResourceTests")
class DropwizardResourceTestsTest {

    // Logback sentinel added BEFORE building the ResourceExtension ---
    private static final LoggerContext LOGBACK_CTX = (LoggerContext) LoggerFactory.getILoggerFactory();
    private static final Logger ROOT_LOGBACK_LOGGER = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    private static final ListAppender<ILoggingEvent> SENTINEL = new ListAppender<>();

    static {
        SENTINEL.setName("KIWI_SENTINEL");
        SENTINEL.setContext(LOGBACK_CTX);
        SENTINEL.start();
        ROOT_LOGBACK_LOGGER.addAppender(SENTINEL);
    }

    @Path("/test")
    public static class MyTestResource {
        @GET
        public Response hello() {
            return Response.ok("hello").build();
        }
    }

    @AfterAll
    static void afterAll() {
        ROOT_LOGBACK_LOGGER.detachAppender(SENTINEL);
        SENTINEL.stop();
    }

    @Nested
    @ExtendWith(DropwizardExtensionsSupport.class)
    class ResourceExtensionFor {

        private static final ResourceExtension RESOURCES =
                DropwizardResourceTests.resourceExtensionFor(new MyTestResource());

        @Test
        void shouldCreateRegisteredResource() {
            var response = RESOURCES.target("/test").request().get();

            assertAll(
                    () -> assertOkResponse(response),
                    () -> assertThat(response.readEntity(String.class)).isEqualTo("hello"));
        }

        @Test
        void shouldNotAllowNullResourceObject() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> DropwizardResourceTests.resourceExtensionFor(null))
                    .withMessage("resource must not be null");
        }

        @Test
        void shouldPreserveLogbackConfigurationByNotBootstrappingLogging() {
            var stillThere = ROOT_LOGBACK_LOGGER.getAppender("KIWI_SENTINEL");
            assertThat(stillThere)
                    .describedAs("Sentinel appender should remain attached; if this assertion fails, " +
                            "Dropwizard likely bootstrapped logging and reset Logback (which detaches appenders).")
                    .isSameAs(SENTINEL);

            SENTINEL.list.clear();
            var marker = MarkerFactory.getMarker("KIWI_SENTINEL_MARKER");
            var logMessage = "kiwi sentinel check";
            ROOT_LOGBACK_LOGGER.warn(marker, logMessage);  // root logger is at WARN level, so must be that level or higher
            assertThat(SENTINEL.list)
                    .describedAs("Expected exactly one WARN message with marker '%s' captured by sentinel; logger=%s, root level=%s. " +
                                    "If empty, Dropwizard likely bootstrapped logging and reset Logback (detaching appenders). " +
                                    "If size != 1, unrelated WARN logs may have been captured.",
                            marker.getName(), ROOT_LOGBACK_LOGGER.getName(), ROOT_LOGBACK_LOGGER.getLevel())
                    .filteredOn(event -> {
                        var markers = event.getMarkerList();
                        return hasOneElement(markers) && first(markers).equals(marker);
                    })
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .containsExactly(logMessage);
        }
    }

    @Nested
    class ResourceBuilderPreservingLogbackConfig {

        private ResourceExtension.Builder builder;

        @BeforeEach
        void setUp() {
            builder = DropwizardResourceTests.resourceBuilderPreservingLogbackConfig();
        }

        @Test
        void shouldCreateResourceExtensionBuilder() {
            assertThat(builder).isNotNull();
        }

        @ClearBoxTest("this directly accesses a private field and assumes a specific class structure")
        void shouldSet_bootstrapLogging_toFalse() throws NoSuchFieldException, IllegalAccessException {
            var field = builder.getClass().getSuperclass().getDeclaredField("bootstrapLogging");
            assertThat(field)
                    .describedAs("We did not find the 'bootstrapLogging' field. Dropwizard might have moved it.")
                    .isNotNull();

            field.setAccessible(true);

            var value = field.get(builder);
            var booleanValue = assertIsExactType(value, Boolean.class);
            assertThat(booleanValue)
                    .describedAs("bootstrapLogging should be false")
                    .isFalse();
        }
    }

}
