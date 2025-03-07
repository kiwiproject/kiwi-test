package org.kiwiproject.test.junit.jupiter.params.provider;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.AnnotationConsumer;
import org.kiwiproject.test.junit.jupiter.params.provider.RandomStringSource.CountStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * An {@link ArgumentsProvider} that provides random strings. Accepts a {@link RandomStringSource}
 * which provides the various options for generating the random strings.
 */
public class RandomStringArgumentsProvider implements ArgumentsProvider, AnnotationConsumer<RandomStringSource> {

    private RandomStringSource randomStringSource;

    @Override
    public void accept(RandomStringSource randomStringSource) {
        this.randomStringSource = randomStringSource;
    }

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        checkArgument(randomStringSource.minLength() <= randomStringSource.maxLength(),
                "minLength must be equal or less than maxLength");

        var prefix = randomStringSource.prefix();
        var suffix = randomStringSource.suffix();
        var randomGenerator = getRandomStringGenerator();
        var stringSupplier = randomSupplier(randomGenerator);
        var argumentCount = getNumberToGenerate();

        return Stream.generate(stringSupplier)
                .map(s -> isNotBlank(prefix) ? prefix + s : s)
                .map(s -> isNotBlank(suffix) ? s + suffix : s)
                .map(Arguments::of)
                .limit(argumentCount);
    }

    @SuppressWarnings("java:S2245")  // suppress: Using pseudorandom number generators (PRNGs) is security-sensitive
    private RandomStringUtils getRandomStringGenerator() {
        return useSecureRandom() ? RandomStringUtils.secure() : RandomStringUtils.insecure();
    }

    private Supplier<String> randomSupplier(RandomStringUtils randomGenerator) {
        var minLengthInclusive = randomStringSource.minLength();
        var maxLengthExclusive = 1 + randomStringSource.maxLength();

        return switch (randomStringSource.stringType()) {
            case ALPHABETIC -> () -> randomGenerator.nextAlphabetic(minLengthInclusive, maxLengthExclusive);
            case ALPHANUMERIC -> () -> randomGenerator.nextAlphanumeric(minLengthInclusive, maxLengthExclusive);
            case ASCII -> () -> randomGenerator.nextAscii(minLengthInclusive, maxLengthExclusive);
            case CHARS -> () -> charsStringSupplier(randomGenerator, minLengthInclusive, maxLengthExclusive);
            case CHAR_RANGE -> charRangeStringSupplier(randomGenerator, minLengthInclusive, maxLengthExclusive);
            case CHAR_RANGES -> charRangesStringSupplier(randomGenerator, minLengthInclusive, maxLengthExclusive);
            case GRAPH -> () -> randomGenerator.nextGraph(minLengthInclusive, maxLengthExclusive);
            case NUMERIC -> () -> randomGenerator.nextNumeric(minLengthInclusive, maxLengthExclusive);
            case PRINT -> () -> randomGenerator.nextPrint(minLengthInclusive, maxLengthExclusive);
        };
    }

    private String charsStringSupplier(RandomStringUtils randomGenerator,
                                       int minLengthInclusive,
                                       int maxLengthExclusive) {

        checkArgument(randomStringSource.chars().length > 0, "chars must have at least one character");
        var length = getRandomUtils().randomInt(minLengthInclusive, maxLengthExclusive);
        return randomGenerator.next(length, randomStringSource.chars());
    }

    private Supplier<String> charRangeStringSupplier(RandomStringUtils randomGenerator,
                                                     int minLengthInclusive,
                                                     int maxLengthExclusive) {

        checkArgument(randomStringSource.endChar() > randomStringSource.beginChar(),
                "endChar must be higher than beginChar");

        var chars = newCharArray(randomStringSource.beginChar(), randomStringSource.endChar());
        return () -> {
            var length = getRandomUtils().randomInt(minLengthInclusive, maxLengthExclusive);
            return randomGenerator.next(length, chars);
        };
    }

    private Supplier<String> charRangesStringSupplier(RandomStringUtils randomGenerator,
                                                      int minLengthInclusive,
                                                      int maxLengthExclusive) {

        var beginChars = randomStringSource.beginChars();
        var endChars = randomStringSource.endChars();
        checkArgument(beginChars.length == endChars.length, "beginChars and endChars must have the same length");

        var charArrayList = new ArrayList<char[]>(beginChars.length);

        for (var i = 0; i < beginChars.length; i++) {
            var beginChar = beginChars[i];
            var endChar = endChars[i];
            var chars = newCharArray(beginChar, endChar);
            charArrayList.add(chars);
        }

        var allChars = combineCharArrays(charArrayList);
        return () -> {
            var length = getRandomUtils().randomInt(minLengthInclusive, maxLengthExclusive);
            return randomGenerator.next(length, allChars);
        };
    }

    private static char[] newCharArray(char beginChar, char endChar) {
        var size = endChar - beginChar + 1;
        var charArray = new char[size];

        for (int i = 0; i < size; i++) {
            charArray[i] = (char) (beginChar + i);
        }
        return charArray;
    }

    private static char[] combineCharArrays(List<char[]> charArrays) {
        var totalLength = charArrays.stream().mapToInt(arr -> arr.length).sum();
        var result = new char[totalLength];

        var destPos = 0;
        for (var charArray : charArrays) {
            System.arraycopy(charArray, 0, result, destPos, charArray.length);
            destPos += charArray.length;
        }
        return result;
    }

    private int getNumberToGenerate() {
        if (randomStringSource.countStrategy() == CountStrategy.FIXED) {
            checkArgument(randomStringSource.count() > 0, "count must be greater than zero");
            return randomStringSource.count();
        }

        checkArgument(randomStringSource.minCount() > 0 && randomStringSource.maxCount() >= randomStringSource.minCount(),
                "minCount must be greater than zero, and maxCount must be greater or equal to minCount");
        var minInclusive = randomStringSource.minCount();
        var maxExclusive = 1 + randomStringSource.maxCount();
        return getRandomUtils().randomInt(minInclusive, maxExclusive);
    }

    @SuppressWarnings("java:S2245")  // suppress: Using pseudorandom number generators (PRNGs) is security-sensitive
    private RandomUtils getRandomUtils() {
        return useSecureRandom() ? RandomUtils.secure() : RandomUtils.insecure();
    }

    private boolean useSecureRandom() {
        return randomStringSource.randomSecurity().isSecure();
    }
}
