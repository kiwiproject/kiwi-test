package org.kiwiproject.test.jdbi;

import lombok.experimental.UtilityClass;
import org.skife.jdbi.v2.GeneratedKeys;
import org.skife.jdbi.v2.Update;

import java.util.Map;

/**
 * Utilities for extracting generated keys from JDBI 2 {@link GeneratedKeys} objects.
 */
@UtilityClass
public class JdbiGeneratedKeys {

    /**
     * Extract the value of a generated key named "id" as a Long.
     *
     * @param generatedKeys a {@link GeneratedKeys} containing a map of fields to value
     * @return the generated key value
     * @see Update#executeAndReturnGeneratedKeys()
     * @see #generatedKey(GeneratedKeys, String)
     */
    public static Long generatedId(GeneratedKeys<Map<String, Object>> generatedKeys) {
        return generatedKey(generatedKeys, "id");
    }

    /**
     * Extract the value of the first generated key named {@code keyName}.
     *
     * @param generatedKeys a {@link GeneratedKeys} containing a map of fields to value
     * @param keyName       the name of the key to retrieve
     * @param <T>           the type of the key to retrieve
     * @return the generated key value
     * @see Update#executeAndReturnGeneratedKeys()
     * @see GeneratedKeys#first()
     */
    @SuppressWarnings("unchecked")
    public static <T> T generatedKey(GeneratedKeys<Map<String, Object>> generatedKeys, String keyName) {
        return (T) generatedKeys.first().get(keyName);
    }
}