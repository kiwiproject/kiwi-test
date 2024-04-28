package org.kiwiproject.test.xmlunit;

import static org.kiwiproject.base.KiwiPreconditions.requireNotNull;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.junit.jupiter.api.TestInfo;
import org.xmlunit.assertj.CompareAssert;
import org.xmlunit.assertj.XmlAssert;
import org.xmlunit.diff.ComparisonController;
import org.xmlunit.diff.ComparisonControllers;
import org.xmlunit.diff.ComparisonListener;

/**
 * KiwiXmlAssert provides convenience on top of <a href="https://www.xmlunit.org">XMLUnit</a> to log
 * any differences found using a {@link LoggingComparisonListener}. Optionally, you can specify a test
 * name that will be included when logging differences.
 * <p>
 * Once the terminal {@link #and(Object)} method is called, you use XMLUnit as usual.
 * Or, you can use one of the convenience methods such as {@link #isIdenticalTo(Object)}
 * which are useful for some common comparison scenarios.
 */
public class KiwiXmlAssert {

    private final Object o;
    private String testName;

    private KiwiXmlAssert(Object o) {
        this.o = requireNotNull(o, "object to compare against must not bul null");
    }

    /**
     * This is an entrance point to begin an XML comparison.
     * <p>
     * Returns a KiwiXmlAssert object for making assertions on the given object.
     *
     * @param o object with a type supported by {@link org.xmlunit.builder.Input#from(Object)}
     * @return a new KiwiXmlAssert object
     */
    public static KiwiXmlAssert assertThat(Object o) {
        return new KiwiXmlAssert(o);
    }

    /**
     * This is an alternate entrance point to begin an XML comparison. It is useful in
     * tests which already statically import AssertJ's {@code assertThat}, because
     * it avoids ambiguity and compilation errors while allowing it to be statically
     * imported. For example, assuming this method has been statically imported:
     * <pre>
     * // inside a test
     * assertThatXml(someXml).isIdenticalTo(someOtherXml);
     * </pre>
     * <p>
     * Returns a KiwiXmlAssert object for making assertions on the given object.
     *
     * @param o object with a type supported by {@link org.xmlunit.builder.Input#from(Object)}
     * @return a new KiwiXmlAssert object
     */
    public static KiwiXmlAssert assertThatXml(Object o) {
        return assertThat(o);
    }

    /**
     * Sets the test name for logging XML differences during comparison.
     *
     * @param testInfo the TestInfo object containing information about the current test
     * @return this KiwiXmlAssert object updated with the updated test name
     */
    public KiwiXmlAssert withTestNameFrom(TestInfo testInfo) {
        return withTestName(requireNotNull(testInfo).getDisplayName());
    }

    /**
     * Sets the test name for logging XML differences during comparison.
     *
     * @param testName the name of the test
     * @return this KiwiXmlAssert object updated with the modified test name
     */
    public KiwiXmlAssert withTestName(String testName) {
        this.testName = testName;
        return this;
    }

    /**
     * This is a terminal operation in {@link KiwiXmlAssert}. It returns a {@link CompareAssert}
     * that has been configured with a {@link LoggingComparisonListener} and the
     * {@link ComparisonControllers#Default Default} comparison controller.
     * <p>
     * If needed, the {@link ComparisonController} can be overridden simply by calling
     * {@link CompareAssert#withComparisonController(ComparisonController)}
     * on the CompareAssert returned by this method. <em>Note, however, that if you use
     * a different ComparisonController which stops at the first difference, then
     * only that first difference will be logged.</em>
     * <p>
     * Additional {@link org.xmlunit.diff.ComparisonListener comparison listeners} can be
     * added by calling {@link CompareAssert#withComparisonListeners(ComparisonListener...)}
     * and supplying additional listeners.
     *
     * @param control the object to compare against this instance's object
     * @return a CompareAssert object for making further assertions on XML differences
     * @see CompareAssert
     * @see ComparisonControllers#Default
     */
    public CompareAssert and(Object control) {
        return XmlAssert.assertThat(o)
                .and(control)
                .withComparisonController(ComparisonControllers.Default)
                .withComparisonListeners(new LoggingComparisonListener(testName));
    }

    /**
     * This is a convenience terminal operation in {@link KiwiXmlAssert} built using
     * {@link #and(Object)} that checks that the control object is identical to this
     * instance's object.
     *
     * @param control the object to compare against this instance's object
     * @return a CompareAssert object for making further assertions on XML differences
     * @see CompareAssert#areIdentical()
     */
    @CanIgnoreReturnValue
    public CompareAssert isIdenticalTo(Object control) {
        return and(control).areIdentical();
    }

    /**
     * This is a convenience terminal operation in {@link KiwiXmlAssert} built using
     * {@link #and(Object)} that checks that the control object is identical to this
     * instance's object, ignoring extra whitespace.
     *
     * @param control the object to compare against this instance's object
     * @return a CompareAssert object for making further assertions on XML differences
     * @see CompareAssert#ignoreWhitespace()
     * @see CompareAssert#areIdentical()
     */
    @CanIgnoreReturnValue
    public CompareAssert isIdenticalToIgnoringWhitespace(Object control) {
        return and(control).ignoreWhitespace().areIdentical();
    }

    /**
     * This is a convenience terminal operation in {@link KiwiXmlAssert} built using
     * {@link #and(Object)} that checks that the control object is identical to this
     * instance's object, ignoring XML comments.
     *
     * @param control the object to compare against this instance's object
     * @return a CompareAssert object for making further assertions on XML differences
     * @see CompareAssert#ignoreComments()
     * @see CompareAssert#areIdentical()
     */
    @CanIgnoreReturnValue
    public CompareAssert isIdenticalToIgnoringComments(Object control) {
        return and(control).ignoreComments().areIdentical();
    }

    /**
     * This is a convenience terminal operation in {@link KiwiXmlAssert} built using
     * {@link #and(Object)} that checks that the control object is identical to this
     * instance's object, ignoring both whitespace and comments.
     *
     * @param control the object to compare against this instance's object
     * @return a CompareAssert object for making further assertions on XML differences
     * @see CompareAssert#ignoreWhitespace()
     * @see CompareAssert#ignoreComments()
     * @see CompareAssert#areIdentical()
     */
    @CanIgnoreReturnValue
    public CompareAssert isIdenticalToIgnoringWhitespaceAndComments(Object control) {
        return and(control).ignoreWhitespace().ignoreComments().areIdentical();
    }
}
