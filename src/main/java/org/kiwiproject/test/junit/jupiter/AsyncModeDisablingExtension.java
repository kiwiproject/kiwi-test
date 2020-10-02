package org.kiwiproject.test.junit.jupiter;

import static org.kiwiproject.concurrent.Async.Mode.DISABLED;
import static org.kiwiproject.concurrent.Async.Mode.ENABLED;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.kiwiproject.concurrent.Async;

/**
 * A JUnit Jupiter {@link org.junit.jupiter.api.extension.Extension Extension} to use in situations where code under
 * test is using Kiwi {@link Async} which lets you easily turn async behavior on and off using the
 * {@link Async#setUnitTestAsyncMode(Async.Mode)} method.
 * <p>
 * This extension disables asynchronous behavior in {@link Async} before each test, then re-enables it after each
 * test.
 * <p>
 * You should only use it in cases when changing the code under test from asynchronous to synchronous behavior
 * will not change the results of a method. For example, a common situation where we use this is HTTP endpoints
 * that receive a payload, use {@link Async} to perform some operation asynchronously, and immediately return a
 * {@code 202 Accepted} response with some kind of status entity to allow client to check back about the status. In
 * a situation like this, we think that changing the background operation to synchronous during a unit test is
 * acceptable, and makes testing easier so long as the operation in question does not take long during the test, for
 * example maybe in the test it calls a mocked out business service class.
 * <p>
 * Use like this:
 * <pre>
 * {@literal @}ExtendWith(AsyncModeDisablingExtension.class)
 *  class YourUnitTest {
 *      // tests...
 *  }
 * </pre>
 *
 * @implNote As currently implemented this will not work if tests are run in parallel.
 */
@Slf4j
public class AsyncModeDisablingExtension implements BeforeEachCallback, AfterEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        LOG.trace("Setting async mode to DISABLED");
        Async.setUnitTestAsyncMode(DISABLED);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        LOG.trace("Setting async mode to ENABLED");
        Async.setUnitTestAsyncMode(ENABLED);
    }
}
