package org.kiwiproject.test.junit.jupiter;

import static java.util.Objects.nonNull;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Static utilities related to JUnit Jupiter.
 * <p>
 * Currently, this is only for internal usage only.
 */
@UtilityClass
@Slf4j
class JupiterHelpers {

    /**
     * Check if the class associated with the current test or container is
     * annotated with {@link Nested}.
     * <p>
     * If there is no test class, tries a fallback mechanism which inspects
     * the unique ID of the context to see if it contains "nested-class". This
     * fallback is admittedly brittle since it relies on the value of a constant
     * in an internal JUnit API (NestedClassTestDescriptor.SEGMENT_TYPE) which is
     * not exported from its module definition.
     *
     * @param context the extension context
     * @return true if the context has a test class and is annotated with {@link Nested};
     * false if there is no test class associated with the context, or if the context
     * unique ID does not contain "nested-class"
     * @implNote The fallback mechanism is brittle and relies on internal JUnit
     * implementation details which are subject to change and therefore make the
     * fallback return false even in cases where it should be true.
     */
    static boolean isTestClassNested(ExtensionContext context) {
        var testClass = context.getTestClass().orElse(null);
        if (nonNull(testClass)) {
            return testClass.isAnnotationPresent(Nested.class);
        }

        LOG.warn("No test class exists for context {} ;" +
                " trying 'nested-class' fallback to determine if we're in a @Nested test class",
                context.getUniqueId());

        return context.getUniqueId().contains("nested-class");
    }

    /**
     * Returns the name of the class associated with the current test or container, or
     * null if one does not exist.
     *
     * @param context the extension context
     * @return the test class name
     */
    static String testClassNameOrNull(ExtensionContext context) {
        return context.getTestClass().map(Class::getName).orElse(null);
    }
}
