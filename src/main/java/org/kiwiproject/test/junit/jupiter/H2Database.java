package org.kiwiproject.test.junit.jupiter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link H2Database} can be used to annotate a parameter of type {@link org.kiwiproject.test.h2.H2FileBasedDatabase}
 * in lifecycle or test methods.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface H2Database {
}
