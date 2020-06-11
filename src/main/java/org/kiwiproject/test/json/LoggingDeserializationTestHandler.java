package org.kiwiproject.test.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A Jackson {@link DeserializationProblemHandler} that logs at WARN level whenever a property with an unknown name is
 * encountered. This can aid in debugging de-serialization problems during tests. It also logs a more comprehensive
 * warning message at INFO level, and if you have a lot of unknown property errors there will be a lot of logging
 * unless you change the log level for this class to WARN or higher.
 * <p>
 * Note that unknown property handling behavior depends on the configuration of the
 * {@link com.fasterxml.jackson.databind.ObjectMapper ObjectMapper}, specifically whether or not the
 * {@link com.fasterxml.jackson.databind.DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES FAIL_ON_UNKNOWN_PROPERTIES}
 * de-serialization feature is enabled or not. It also depends on whether the {@code ignoreUnknown} property
 * has been set to true in {@link com.fasterxml.jackson.annotation.JsonIgnoreProperties JsonIgnoreProperties}.
 * <p>
 * This requires that jackson-core and jackson-databind are available at runtime.
 */
@Slf4j
public class LoggingDeserializationTestHandler extends DeserializationProblemHandler {

    private static final String LINE_SEPARATOR = System.lineSeparator();

    private static final String WARNING_MESSAGE = LINE_SEPARATOR +
            "--------------------------------------------------------------------------------" + LINE_SEPARATOR +
            "To avoid these errors, you can: " + LINE_SEPARATOR +
            "* Use @JsonIgnoreProperties(ignoreUnknown = true)" + LINE_SEPARATOR +
            "* Disable DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES on the ObjectMapper" + LINE_SEPARATOR +
            "* Use kiwi's org.kiwiproject.json.LoggingDeserializationProblemHandler)" + LINE_SEPARATOR +
            "--------------------------------------------------------------------------------";

    private final AtomicLong unknownPropertyCounter = new AtomicLong();

    @Override
    public boolean handleUnknownProperty(DeserializationContext ctxt,
                                         JsonParser parser,
                                         JsonDeserializer<?> deserializer,
                                         Object beanOrClass,
                                         String propertyName) throws IOException {

        var path = parser.getParsingContext().pathAsPointer().toString().replace("/", ".");
        var className = beanOrClass.getClass().getName();

        LOG.warn("Unable to deserialize path: '{}' for class: {}", path, className);
        LOG.info(WARNING_MESSAGE);

        unknownPropertyCounter.incrementAndGet();

        return super.handleUnknownProperty(ctxt, parser, deserializer, beanOrClass, propertyName);
    }

    /**
     * Returns the total count of ALL unknown properties encountered; in other words if the same unknown property
     * is encountered 10 times, and a second unknown property is encountered 5 times, this method will return 15.
     * This is not likely to be all that useful, other than to provide a general sense for the number of unknown
     * properties. It will be more useful when running a single unit test.
     *
     * @return the total count of all unknown properties encountered
     */
    public long getErrorCount() {
        return unknownPropertyCounter.get();
    }
}
