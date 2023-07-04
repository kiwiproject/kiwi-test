package org.kiwiproject.test;

import liquibase.command.core.AbstractUpdateCommandStep;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class PidLogger {

    public static void logCurrentPid() {
        LOG.warn("Current pid: {}", ProcessHandle.current().pid());

        // Also, if possible log the AbstractUpdateCommandStep#upToDateFastCheck (static) field
        try {
            var field = AbstractUpdateCommandStep.class.getDeclaredField("upToDateFastCheck");
            field.setAccessible(true);
            var upToDateFastCheck = field.get(null);

            LOG.warn("upToDateFastCheck: {}", upToDateFastCheck);
        } catch (Exception e) {
            LOG.warn("Unable to get field AbstractUpdateCommandStep.upToDateFastCheck");
        }
    }
}
