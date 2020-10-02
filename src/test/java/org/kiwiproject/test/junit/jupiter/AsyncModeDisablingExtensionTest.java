package org.kiwiproject.test.junit.jupiter;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kiwiproject.base.DefaultEnvironment;
import org.kiwiproject.concurrent.Async;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@ExtendWith(AsyncModeDisablingExtension.class)
class AsyncModeDisablingExtensionTest {

    @Test
    void shouldDisableAsyncModeDuringThisTest() {
        int sleepTime = 100;
        long startNanos = System.nanoTime();
        Async.doAsync(() -> {
            new DefaultEnvironment().sleepQuietly(sleepTime, TimeUnit.MILLISECONDS);
            return 42;
        });

        long elapsedNanos = System.nanoTime() - startNanos;
        long elapsedMillis = Duration.ofNanos(elapsedNanos).toMillis();
        LOG.info("Elapsed time: {} millis", elapsedMillis);

        assertThat(elapsedMillis)
                .describedAs("Elapsed time should be >= sleep time when async turned off")
                .isGreaterThanOrEqualTo(sleepTime);
    }
}
