package org.kiwiproject.test.xmlunit;

import static java.util.Objects.nonNull;
import static org.kiwiproject.base.KiwiPreconditions.requireNotNull;

import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.xmlunit.diff.Comparison;
import org.xmlunit.diff.ComparisonListener;
import org.xmlunit.diff.ComparisonResult;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A listener for XML comparison events that logs any differences using a logger.
 *
 * @see ComparisonListener
 */
@Slf4j
public class LoggingComparisonListener implements ComparisonListener {

    private final String testName;
    private final AtomicBoolean haveSeenDifferenceAlready;

    /**
     * Private {@link Logger} used for logging XML differences. By default, it is
     * the {@code static final} Logger for this class, but can be changed using the
     * {@link #withLogger(Logger)} method.
     */
    @SuppressWarnings("NonConstantLogger")
    private Logger logger = LOG;

    /**
     * Create a new instance without an explicit test name.
     */
    public LoggingComparisonListener() {
        this((String) null);
    }

    /**
     * Create a new instance with a test name extracted from the given TestInfo.
     *
     * @param testInfo the JUnit {@link TestInfo}
     */
    public LoggingComparisonListener(TestInfo testInfo) {
        this(requireNotNull(testInfo).getDisplayName());
    }

    /**
     * Create a new instance with the given test name.
     *
     * @param testName the test name
     */
    public LoggingComparisonListener(@Nullable String testName) {
        this.testName = testName;
        this.haveSeenDifferenceAlready = new AtomicBoolean();
    }

    /**
     * Sets the logger to be used for logging XML differences.
     * <p>
     * If this is not called, a default logger is used.
     *
     * @param logger the logger to set
     * @return the current instance of {@link LoggingComparisonListener}
     */
    public LoggingComparisonListener withLogger(Logger logger) {
        this.logger = requireNotNull(logger);
        return this;
    }

    /**
     * This method is called when a comparison is performed. If the outcome of the comparison is DIFFERENT,
     * it logs the difference, including a test name if it was provided to this instance.
     *
     * @param comparison the comparison object
     * @param outcome    the outcome of the comparison
     */
    @Override
    public void comparisonPerformed(Comparison comparison, ComparisonResult outcome) {
        if (outcome == ComparisonResult.DIFFERENT) {
            logDifference(comparison);
        }
    }

    @SuppressWarnings("java:S2629")
    private void logDifference(Comparison comparison) {
        if (haveSeenDifferenceAlready.compareAndSet(false, true)) {
            logPreamble();
        }

        logger.warn(comparison.toString());
    }

    private void logPreamble() {
        var testNameOrEmpty = nonNull(testName) ? (testName + ": ") : "";
        logger.warn("{}XML differences found:", testNameOrEmpty);
    }
}
