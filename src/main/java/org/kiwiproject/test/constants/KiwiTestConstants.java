package org.kiwiproject.test.constants;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import io.dropwizard.jackson.Jackson;
import lombok.experimental.UtilityClass;
import org.kiwiproject.json.JsonHelper;
import org.kiwiproject.test.json.LoggingDeserializationTestHandler;

/**
 * A utility class that provides some rather opinionated constants for use in test code.
 * <p>
 * The advantage of using this is (assuming you're OK with the opinions used) that all test
 * code will use a consistent set of behaviors.
 */
@UtilityClass
public class KiwiTestConstants {

    /**
     * An {@link ObjectMapper} that can be used in tests. The mapper is created using
     * {@link JsonHelper#newDropwizardObjectMapper()} which in turn relies on Dropwizard's
     * {@link Jackson#newObjectMapper()} method and how it configures the mapper.
     * <p>
     * As of Dropwizard 2.x, the behavior has been changed such that
     * {@link com.fasterxml.jackson.databind.DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES FAIL_ON_UNKNOWN_PROPERTIES}
     * is <em>false</em> by default now, whereas in previous Dropwizard versions (and in Jackson) the default is <em>true</em>.
     * <p>
     * NOTE: Since this is static, changes to it may persist across tests and cause unexpected behavior. Modify
     * at your own risk. The recommended usage is as a "read-only" object to use for JSON serialization.
     *
     * @see JsonHelper#newDropwizardObjectMapper()
     * @see Jackson#newObjectMapper()
     * @see com.fasterxml.jackson.databind.DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES
     */
    public static final ObjectMapper OBJECT_MAPPER = JsonHelper.newDropwizardObjectMapper();

    /**
     * A Jackson {@link DeserializationProblemHandler} that can be added to an {@link ObjectMapper} in order to
     * log and count unknown properties that occur during JSON deserialization.
     * <p>
     * This is added to {@link #OBJECT_MAPPER}.
     */
    public static final LoggingDeserializationTestHandler JSON_DESERIALIZATION_HANDLER =
            new LoggingDeserializationTestHandler();

    static {
        OBJECT_MAPPER.addHandler(JSON_DESERIALIZATION_HANDLER);
    }

    /**
     * A Kiwi {@link JsonHelper} that can be used in tests. It uses {@link #OBJECT_MAPPER}, which has a behavior
     * defined by Dropwizard instead of the default Jackson {@link ObjectMapper}.
     *
     * @see #OBJECT_MAPPER
     * @see #JSON_DESERIALIZATION_HANDLER
     */
    public static final JsonHelper JSON_HELPER = new JsonHelper(OBJECT_MAPPER);
}
