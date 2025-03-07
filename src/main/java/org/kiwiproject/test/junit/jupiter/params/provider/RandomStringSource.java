package org.kiwiproject.test.junit.jupiter.params.provider;

import org.junit.jupiter.params.provider.ArgumentsSource;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An {@link ArgumentsSource} that provides random strings to parameterized tests.
 */
@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ArgumentsSource(RandomStringArgumentsProvider.class)
public @interface RandomStringSource {

    /**
     * The minimum length that a generated argument will have.
     */
    int minLength() default 5;

    /**
     * The maximum length that a generated argument will have.
     */
    int maxLength() default 20;

    /**
     * A prefix that will be prepended to each argument.
     */
    String prefix() default "";

    /**
     * A suffix that will be appended to each argument.
     */
    String suffix() default "";

    /**
     * Controls the type of characters that the generated strings contain.
     * <p>
     * Depending on the type, you can specify additional properties that
     * control, for example, the range of characters to include.
     */
    StringType stringType() default StringType.ALPHANUMERIC;

    /**
     * Defines whether the PRNG used to generate random strings is
     * cryptographically strong.
     */
    RandomSecurity randomSecurity() default RandomSecurity.INSECURE;

    /**
     * Defines a fixed number of arguments to generate.
     * <p>
     * Use with {@link CountStrategy#FIXED}.
     */
    int count() default 10;

    /**
     * Defines a minimum number of arguments to generate.
     * <p>
     * Use with {@link CountStrategy#RANDOM}.
     */
    int minCount() default 1;

    /**
     * Defines a maximum number of arguments to generate.
     * <p>
     * Use with {@link CountStrategy#RANDOM}.
     */
    int maxCount() default 10;

    /**
     * Defines the specific characters that a random string may contain.
     * <p>
     * Use with {@link StringType#CHARS}.
     */
    char[] chars() default {
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'
    };

    /**
     * Defines the beginning character in a range that defines the
     * specific characters that a random string may contain.
     * <p>
     * Use with {@link StringType#CHAR_RANGE}.
     */
    char beginChar() default 'a';

    /**
     * Defines the ending character in a range that defines the
     * specific characters that a random string may contain.
     * <p>
     * Use with {@link StringType#CHAR_RANGE}.
     */
    char endChar() default 'z';

    /**
     * Defines the beginning characters of one or more ranges that define the
     * specific characters that a random string may contain.
     * <p>
     * Use with {@link StringType#CHAR_RANGES}.
     */
    char[] beginChars() default { 'a', 'A', '0' };

    /**
     * Defines the ending characters of one or more ranges that define the
     * specific characters that a random string may contain.
     * <p>
     * Use with {@link StringType#CHAR_RANGES}.
     */
    char[] endChars() default { 'z', 'Z', '9' };

    /**
     * The strategy that determines the number of arguments to provide.
     */
    CountStrategy countStrategy() default CountStrategy.FIXED;

    /**
     * Defines whether to generate a fixed or variable number of arguments.
     */
    enum CountStrategy {

        /**
         * Generate a fixed number of arguments.
         */
        FIXED,

        /**
         * Generate a random number of arguments between a minimum and maximum.
         */
        RANDOM
    }

    /**
     * Defines the types of characters to include in the random strings.
     */
    enum StringType {

        /**
         * Characters from Latin alphabetic characters (a-z, A-Z) will be used.
         */
        ALPHABETIC,

        /**
         * Characters from Latin alphabetic characters (a-z, A-Z) and the digits 0-9 will be used.
         */
        ALPHANUMERIC,

        /**
         * Characters from the ASCII character set with values between 32 and 126 (inclusive) will be used.
         */
        ASCII,

        /**
         * A specific set of user-specified characters.
         *
         * @see #chars()
         */
        CHARS,

        /**
         * Characters in a user-specified range.
         *
         * @see #beginChar()
         * @see #endChar()
         */
        CHAR_RANGE,

        /**
         * Characters in one or more user-specified ranges.
         * <p>
         * The ranges are defined using {@link #beginChars()} and {@link #endChars()}
         * at corresponding indexes.
         * <p>
         * For example, if {@code beginChars} is {@code ['a', t', '4']} and {@code endChars}
         * is {@code ['d', 'v', '7']} then the overall set of characters contains
         * {@code 'a'} through {@code 'd'}, {@code 't'} through {@code 'v'}, and
         * {@code '4'} through {@code '7'}. The ranges are inclusive, so the beginning
         * and ending characters are included.
         *
         * @see #beginChars()
         * @see #endChars()
         */
        CHAR_RANGES,

        /**
         * Characters matching the {@code POSIX [:graph:]} regular expression character class.
         * This set of characters contains all visible ASCII characters (i.e., all ASCII
         * characters except spaces and control characters).
         */
        GRAPH,

        /**
         * Characters include the digits 0-9.
         */
        NUMERIC,

        /**
         * Characters matching the {@code POSIX [:print:]} regular expression character class.
         * This set of characters contains all visible ASCII characters and spaces (but no
         * control characters).
         */
        PRINT
    }

    /**
     * Controls whether a cryptographically strong PRNG is used to generate random characters.
     */
    enum RandomSecurity {

        /**
         * Random strings are generated using a cryptographically strong PRNG
         * such as {@link java.security.SecureRandom}.
         */
        SECURE,

        /**
         * Random strings are generated using a PRNG that is not cryptographically
         * strong, such as {@link java.util.concurrent.ThreadLocalRandom}.
         * <p>
         * For unit tests, this will almost always be enough. It will also
         * be, in general, significantly faster when generating large numbers
         * of arguments.
         */
        INSECURE;

        public boolean isSecure() {
            return this == SECURE;
        }
    }
}
