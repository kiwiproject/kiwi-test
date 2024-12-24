package org.kiwiproject.test.assertj.jsonassert;

import org.assertj.core.api.AbstractSoftAssertions;
import org.json.JSONArray;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 * Integrates {@link JSONAssert} into AssertJ's "soft assertions" model by catching assertion failures and
 * manually failing them via {@link AbstractSoftAssertions#fail(String, Throwable)}.
 * <p>
 * Currently only wraps the {@link JSONAssert#assertEquals(String, JSONArray, JSONCompareMode)} method.
 * <p>
 * Note that you need the org.skyscreamer:jsonassert dependency and its dependencies, which as of this writing
 * consists of com.vaadin.external.google:android-json. You need to import {@code org.json} classes like
 * {@link org.json.JSONObject} and {@link JSONException}.
 */
public class JSONAssertSoftAssertions {

    private final AbstractSoftAssertions softAssertions;

    /**
     * Construct instance using the given soft assertions.
     * <p>
     * Note that when using {@link org.assertj.core.api.junit.jupiter.SoftAssertionsExtension} you will have to
     * instantiate a new instance of this in each test, since that extension forces you to declare a
     * {@link org.assertj.core.api.SoftAssertions} argument to each test method where you want soft assertions. This
     * contrasts with the previous way using the deprecated {@link org.assertj.core.api.JUnitJupiterSoftAssertions}
     * which allowed you to store an instance field in your test and register it with JUnit Jupiter's extension
     * mechanism.
     *
     * @param softAssertions the soft assertions to use
     */
    @SuppressWarnings("deprecation")  // we know that we are referencing a deprecated class in the docs
    public JSONAssertSoftAssertions(AbstractSoftAssertions softAssertions) {
        this.softAssertions = softAssertions;
    }

    /**
     * Wrapper around {@link JSONAssert#assertEquals(JSONArray, JSONArray, JSONCompareMode)} that catches any thrown
     * {@link JSONException} or {@link AssertionError} and converts into a failed soft assertion. Performs the
     * comparison using the {@link JSONCompareMode#LENIENT} mode.
     *
     * @param expectedStr the expected JSON string
     * @param actualStr   the actual JSON string
     */
    public void assertEqualsLenient(String expectedStr, String actualStr) {
        assertEquals(expectedStr, actualStr, JSONCompareMode.LENIENT);
    }

    /**
     * Wrapper around {@link JSONAssert#assertEquals(JSONArray, JSONArray, JSONCompareMode)} that catches any thrown
     * {@link JSONException} or {@link AssertionError} and converts into a failed soft assertion.
     *
     * @param expectedStr the expected JSON string
     * @param actualStr   the actual JSON string
     * @param compareMode how to compare
     */
    public void assertEquals(String expectedStr, String actualStr, JSONCompareMode compareMode) {
        try {
            JSONAssert.assertEquals(expectedStr, actualStr, compareMode);
        } catch (AssertionError | JSONException e) {
            softAssertions.fail("JSON assertion failure: " + e.getMessage(), e);
        }
    }
}
