package org.kiwiproject.test.junit.jupiter.params.provider;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.CharUtils.isAscii;
import static org.apache.commons.lang3.CharUtils.isAsciiAlpha;
import static org.apache.commons.lang3.CharUtils.isAsciiAlphanumeric;
import static org.apache.commons.lang3.CharUtils.isAsciiPrintable;
import static org.apache.commons.lang3.StringUtils.containsOnly;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.params.IntRangeSource;
import org.kiwiproject.test.junit.jupiter.params.provider.RandomStringSource.CountStrategy;
import org.kiwiproject.test.junit.jupiter.params.provider.RandomStringSource.RandomSecurity;
import org.kiwiproject.test.junit.jupiter.params.provider.RandomStringSource.StringType;

import java.lang.annotation.Annotation;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

@DisplayName("RandomStringArgumentsProvider")
@Slf4j
class RandomStringArgumentsProviderTest {

    @ParameterizedTest
    @RandomStringSource(randomSecurity = RandomSecurity.SECURE)
    void shouldProvideStrings_UsingSecureRandom(String s) {
        assertThat(s).isNotBlank();
    }

    /**
     * This is NOT intended as a test of the Java PRNGs but rather to
     * verify that the argument provider is using a PRNG at all.
     * <p>
     * Plus, it was interesting coming up with this pseudo-test with the help of ChatGPT.
     * Though, I only skimmed some sources it gave me, such as
     * <a href="https://en.wikipedia.org/wiki/Pseudorandom_number_generator">Pseudorandom number generator</a>,
     * <a href="https://math.stackexchange.com/questions/72223/finding-expected-number-of-distinct-values-selected-from-a-set-of-integers">Finding expected number of distinct values selected from a set of integers</a>,
     * and <a href="https://www.randomservices.org/random/urn/Birthday.html">The Birthday Problem</a>.
     * <p>
     * When writing this test, I observed that, for higher number of arguments generated,
     * the secure random PRNG is significantly slower. For example, on my M2 MacBook Pro
     * this test takes about 10-11 milliseconds to generate 50,000 alphanumeric arguments
     * between 5 and 10 characters long using an insecure PRNG, while it consistently takes
     * between 550 and 600 milliseconds for the secure PRNG! Interestingly, I did not
     * observe significant differences in the number of unique numbers generated, but
     * that is most likely because this "test" is not realistic and generates only a
     * limited number of random values. Also, these observations are NOT meant to imply
     * that the insecure PRNG generates random values that are just as "good" as the secure one.
     */
    @ParameterizedTest
    @CsvSource(textBlock = """
            10, INSECURE, 2, 2
            10, SECURE, 2, 2
            20, INSECURE, 5, 15
            20, SECURE, 5, 15
            1000, INSECURE, 3, 3
            1000, SECURE, 3, 3
            2000, INSECURE, 3, 3
            2000, SECURE, 3, 3
            3000, INSECURE, 3, 3
            3000, SECURE, 3, 3
            5000, INSECURE, 5, 5
            5000, SECURE, 5, 5
            5000, INSECURE, 5, 10
            5000, SECURE, 5, 10
            50000, INSECURE, 5, 5
            100000, INSECURE, 5, 5
            """)
    void shouldEnsureNonDeterministicValues(int argumentCount,
                                            RandomSecurity security,
                                            int minLength,
                                            int maxLength) {

        var randomStringSource = RandomStringSourceForTesting.builder()
                .randomSecurity(security)
                .minLength(minLength)
                .maxLength(maxLength)
                .count(argumentCount)
                .build();

        var provider = createAndInitProvider(randomStringSource);

        var values = provider.provideArguments(null, null)
                .map(Arguments::get)
                .flatMap(Arrays::stream)
                .map(String.class::cast)
                .limit(argumentCount)
                .collect(toSet());

        var minimumPercentUnique = 0.98;
        var expectedMinUnique = (int) (minimumPercentUnique * argumentCount);
        var numUniqueValues = values.size();
        var percentUnique = numUniqueValues / (double) argumentCount;

        var percentInstance = NumberFormat.getPercentInstance(Locale.ENGLISH);
        percentInstance.setMinimumFractionDigits(2);

        LOG.debug("argument count = {}", argumentCount);
        LOG.debug("num unique values = {}", numUniqueValues);
        LOG.debug("num duplicate values = {}", (argumentCount - numUniqueValues));
        LOG.debug("% unique values = {}", percentInstance.format(percentUnique));
        LOG.debug("minimum allowed unique values = {} ({})",
                expectedMinUnique, percentInstance.format(minimumPercentUnique));

        assertThat(values)
                .describedAs("Got %d (%s) unique values but expected a minimum of %d",
                        numUniqueValues,
                        percentInstance.format(percentUnique),
                        expectedMinUnique)
                .hasSizeGreaterThanOrEqualTo(expectedMinUnique);
    }

    @ParameterizedTest
    @RandomStringSource(count = 50)
    void shouldProvideStrings_BetweenDefaultMinAndMax(String s) {
        assertThat(s.length())
                .isGreaterThanOrEqualTo(5)
                .isLessThanOrEqualTo(20);
    }

    @ParameterizedTest
    @IntRangeSource(from = -10, to = 0, closed = true)
    void shouldThrowIllegalArgumentException_IfCountIsNotPositive(int count) {
        var randomStringSource = RandomStringSourceForTesting.builder()
                .count(count)
                .build();

        var provider = createAndInitProvider(randomStringSource);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> provider.provideArguments(null, null))
                .withMessage("count must be greater than zero");
    }

    @ParameterizedTest
    @RandomStringSource(count = 25, minLength = 2, maxLength = 8)
    void shouldProvideStrings_BetweenCustomMinAndMax(String s) {
        assertThat(s.length())
                .isGreaterThanOrEqualTo(2)
                .isLessThanOrEqualTo(8);
    }

    @RepeatedTest(10)
    void shouldThrowIllegalArgumentException_IfMinLengthGreaterThanMaxLength() {
        var minLength = ThreadLocalRandom.current().nextInt(1, 10);
        var maxLength = minLength - 1;

        var randomStringSource = RandomStringSourceForTesting.builder()
                .minLength(minLength)
                .maxLength(maxLength)
                .build();

        var provider = createAndInitProvider(randomStringSource);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> provider.provideArguments(null, null))
                .withMessage("minLength must be equal or less than maxLength");
    }

    @ParameterizedTest
    @RandomStringSource(prefix = "test-")
    void shouldProvideStrings_WithPrefix(String s) {
        assertThat(s).startsWith("test-");
    }

    @ParameterizedTest
    @RandomStringSource(suffix = "-service")
    void shouldProvideStrings_WithSuffix(String s) {
        assertThat(s).endsWith("-service");
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 5, 10, 25, 35 })
    void shouldProduceFixedNumberOfValues(int count) {
        var randomStringSource = RandomStringSourceForTesting.builder()
                .count(count)
                .build();

        var provider = createAndInitProvider(randomStringSource);

        var arguments = provider.provideArguments(null, null)
                .map(Arguments::get)
                .flatMap(Arrays::stream)
                .map(String.class::cast)
                .toArray();

        assertThat(arguments).hasSize(count);
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            1, 10
            5, 15
            5, 5
            10, 20
            5, 30
            10, 50
            """)
    void shouldProduceRandomNumberOfValues(int minCount, int maxCount) {
        var randomStringSource = RandomStringSourceForTesting.builder()
                .minCount(minCount)
                .maxCount(maxCount)
                .countStrategy(CountStrategy.RANDOM)
                .build();

        var provider = createAndInitProvider(randomStringSource);

        var arguments = provider.provideArguments(null, null)
                .map(Arguments::get)
                .flatMap(Arrays::stream)
                .map(String.class::cast)
                .toArray();

        assertThat(arguments).hasSizeBetween(minCount, maxCount);
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            0, 10
            0, 15
            5, 4
            10, 9
            35, 30
            20, 10
            """)
    void shouldThrowIllegalArgumentException_IfInvalidMinAndMaxCount(int minCount, int maxCount) {
        var randomStringSource = RandomStringSourceForTesting.builder()
                .minCount(minCount)
                .maxCount(maxCount)
                .countStrategy(CountStrategy.RANDOM)
                .build();

        var provider = createAndInitProvider(randomStringSource);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> provider.provideArguments(null, null))
                .withMessage("minCount must be greater than zero, and maxCount must be greater or equal to minCount");
    }

    @ParameterizedTest
    @RandomStringSource(stringType = StringType.ALPHABETIC)
    void shouldProduceStrings_WithOnly_AlphabeticCharacters(String s) {
        s.chars().forEach(ch -> assertThat(isAsciiAlpha((char) ch)).isTrue());
    }

    @ParameterizedTest
    @RandomStringSource(stringType = StringType.ALPHANUMERIC)
    void shouldProduceStrings_WithOnly_AlphanumericCharacters(String s) {
        s.chars().forEach(ch -> assertThat(isAsciiAlphanumeric((char) ch)).isTrue());
    }

    @ParameterizedTest
    @RandomStringSource(stringType = StringType.ASCII)
    void shouldProduceStrings_WithOnly_AsciiCharacters(String s) {
        s.chars().forEach(ch -> assertThat(isAscii((char) ch)).isTrue());
    }

    @ParameterizedTest
    @RandomStringSource(stringType = StringType.GRAPH)
    void shouldProduceStrings_WithOnly_GraphCharacters(String s) {
        var validChars = IntStream.rangeClosed(33, 126)
                .mapToObj(i -> String.valueOf((char) i))
                .collect(joining())
                .toCharArray();

        assertThat(containsOnly(s, validChars)).isTrue();
    }

    @ParameterizedTest
    @RandomStringSource(stringType = StringType.NUMERIC)
    void shouldProduceStrings_WithOnly_NumericCharacters(String s) {
        assertThat(s).containsOnlyDigits();
    }

    @ParameterizedTest
    @RandomStringSource(stringType = StringType.PRINT)
    void shouldProduceStrings_WithOnly_PrintCharacters(String s) {
        s.chars().forEach(ch -> assertThat(isAsciiPrintable((char) ch)).isTrue());
    }

    @ParameterizedTest
    @RandomStringSource(stringType = StringType.CHARS, chars = { 'a', 'b', 'x', 'y', 'z' })
    void shouldProduceStrings_WithSpecificChars(String s) {
        assertThat(containsOnly(s, 'a', 'b', 'x', 'y', 'z')).isTrue();
    }

    @Test
    void shouldThrowIllegalArgumentException_IfCharsIsEmpty() {
        var randomStringSource = RandomStringSourceForTesting.builder()
                .stringType(StringType.CHARS)
                .chars(new char[0])
                .build();

        var provider = createAndInitProvider(randomStringSource);

        // we need to force the stream to terminate for this test
        //noinspection ResultOfMethodCallIgnored
        assertThatIllegalArgumentException()
                .isThrownBy(() -> provider.provideArguments(null, null).toList())
                .withMessage("chars must have at least one character");
    }

    @ParameterizedTest
    @RandomStringSource(stringType = StringType.CHAR_RANGE, beginChar = 't', endChar = 'x')
    void shouldProduceStrings_WithinCharRange(String s) {
        assertThat(containsOnly(s, 't', 'u', 'v', 'w', 'x', 'y', 'z')).isTrue();
    }

    @ParameterizedTest
    @RandomStringSource(stringType = StringType.CHAR_RANGE,
            beginChar = 't', endChar = 'x',
            randomSecurity = RandomSecurity.SECURE)
    void shouldProduceStrings_WithinCharRange_UsingSecureRandom(String s) {
        assertThat(containsOnly(s, 't', 'u', 'v', 'w', 'x', 'y', 'z')).isTrue();
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            d, c
            z, a
            f, b
            """)
    void shouldThrowIllegalArgumentException_IfBeginCharIsGreaterThanEndChar(char beginChar, char endChar) {
        var randomStringSource = RandomStringSourceForTesting.builder()
                .stringType(StringType.CHAR_RANGE)
                .beginChar(beginChar)
                .endChar(endChar)
                .build();

        var provider = createAndInitProvider(randomStringSource);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> provider.provideArguments(null, null))
                .withMessage("endChar must be higher than beginChar");
    }

    @ParameterizedTest
    @RandomStringSource(stringType = StringType.CHAR_RANGES,
            beginChars = { 'j', 'J', '5' },
            endChars = { 'p', 'P', '8' })
    void shouldProduceStrings_WithinCharRanges(String s) {
        var validChars = new char[] {
                'j', 'k', 'l', 'm', 'n', 'o', 'p',
                'J', 'K', 'L', 'M', 'N', 'O', 'P',
                '5', '6', '7', '8'
        };

        assertThat(containsOnly(s, validChars)).isTrue();
    }

    @RepeatedTest(10)
    void shouldThrowIllegalArgumentException_IfBeginCharsLengthDoesNotMatchEndCharsLength() {
        var beginCharCount = ThreadLocalRandom.current().nextInt(5, 11);
        var beginChars = new char[beginCharCount];

        var randomVal = ThreadLocalRandom.current().nextInt(-3, 4);
        var diff = (randomVal == 0) ? 1 : randomVal;  // the diff can't be zero!
        var endChars = new char[beginCharCount + diff];

        var randomStringSource = RandomStringSourceForTesting.builder()
                .stringType(StringType.CHAR_RANGES)
                .beginChars(beginChars)
                .endChars(endChars)
                .build();

        var provider = createAndInitProvider(randomStringSource);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> provider.provideArguments(null, null))
                .withMessage("beginChars and endChars must have the same length");
    }

    private static RandomStringArgumentsProvider createAndInitProvider(RandomStringSource randomStringSource) {
        var provider = new RandomStringArgumentsProvider();
        provider.accept(randomStringSource);
        return provider;
    }

    @SuppressWarnings("ClassExplicitlyAnnotation")
    @Builder
    @Getter
    @Accessors(fluent = true)
    static class RandomStringSourceForTesting implements RandomStringSource {

        @Builder.Default
        int minLength = 5;

        @Builder.Default
        int maxLength = 20;

        @Builder.Default
        String prefix = "";

        @Builder.Default
        String suffix = "";

        @Builder.Default
        StringType stringType = StringType.ALPHANUMERIC;

        @Builder.Default
        RandomSecurity randomSecurity = RandomSecurity.INSECURE;

        @Builder.Default
        int count = 5;

        @Builder.Default
        int minCount = 1;

        @Builder.Default
        int maxCount = 10;

        @Builder.Default
        char[] chars = { 'a', 'b', 'c', 'd', 'e' };

        @Builder.Default
        char beginChar = 'a';

        @Builder.Default
        char endChar = 'e';

        @Builder.Default
        char[] beginChars = { 'a', '1' };

        @Builder.Default
        char[] endChars = { 'e', '5' };

        @Builder.Default
        CountStrategy countStrategy = CountStrategy.FIXED;

        @Override
        public Class<? extends Annotation> annotationType() {
            return RandomStringSource.class;
        }
    }
}
