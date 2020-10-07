package org.kiwiproject.test.junit.jupiter;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.kiwiproject.test.h2.H2FileBasedDatabase;

/**
 * A JUnit Jupiter extension solely for testing the {@link H2FileBasedDatabaseExtension}.
 * <p>
 * It requires a {@link H2FileBasedDatabase} and does nothing except log database information.
 */
@AllArgsConstructor
@Slf4j
public class H2DatabaseTestExtension implements BeforeEachCallback, AfterEachCallback {

    @Getter
    private final H2FileBasedDatabase database;

    @Override
    public void beforeEach(ExtensionContext context) {
        LOG.debug("Database in beforeEach: {}", database);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        LOG.debug("Database in afterEach: {}", database);
    }
}
