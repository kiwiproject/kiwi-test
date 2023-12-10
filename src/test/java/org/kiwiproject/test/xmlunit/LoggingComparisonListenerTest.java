package org.kiwiproject.test.xmlunit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.kiwiproject.test.logback.InMemoryAppender;
import org.kiwiproject.test.logback.InMemoryAppenderExtension;
import org.xmlunit.diff.Comparison;
import org.xmlunit.diff.ComparisonResult;

@DisplayName("LoggingComparisonListener")
@Slf4j
class LoggingComparisonListenerTest {

    @RegisterExtension
    private final InMemoryAppenderExtension inMemoryAppenderExtension =
            new InMemoryAppenderExtension(LoggingComparisonListenerTest.class);

    private InMemoryAppender appender;
    private Comparison comparison;

    @BeforeEach
    void setUp() {
        appender = inMemoryAppenderExtension.appender();
        comparison = mock(Comparison.class);
    }

    @ParameterizedTest
    @EnumSource(value = ComparisonResult.class,
            mode = EnumSource.Mode.EXCLUDE,
            names = "DIFFERENT")
    void shouldNotLogWhenComparisonResultIsNotDifferent(ComparisonResult result) {
        var listener = new LoggingComparisonListener().withLogger(LOG);
        listener.comparisonPerformed(comparison, result);

        assertThat(appender.orderedEvents()).isEmpty();
    }

    @Test
    void shouldLogWithNoTestName() {
        var listener = new LoggingComparisonListener().withLogger(LOG);

        when(comparison.toString()).thenReturn("The difference");

        listener.comparisonPerformed(comparison, ComparisonResult.DIFFERENT);

        assertThat(appender.orderedEventMessages())
                .containsExactly("XML differences found:", "The difference");
    }

    @Test
    void shouldLogWithTestName_FromTestInfo() {
        var testInfo = mock(TestInfo.class);
        when(testInfo.getDisplayName()).thenReturn("My Test");

        var listener = new LoggingComparisonListener(testInfo).withLogger(LOG);

        when(comparison.toString()).thenReturn("The difference");

        listener.comparisonPerformed(comparison, ComparisonResult.DIFFERENT);

        assertThat(appender.orderedEventMessages())
                .containsExactly("My Test: XML differences found:", "The difference");
    }

    @Test
    void shouldLogWithTestName() {
        var listener = new LoggingComparisonListener("Some Test").withLogger(LOG);

        when(comparison.toString()).thenReturn("The difference");

        listener.comparisonPerformed(comparison, ComparisonResult.DIFFERENT);

        assertThat(appender.orderedEventMessages())
                .containsExactly("Some Test: XML differences found:", "The difference");
    }
}
