package org.kiwiproject.test.dropwizard.app;

import io.dropwizard.lifecycle.Managed;

public abstract class NoOpManaged implements Managed {

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    public boolean mischiefManaged() {
        return true;
    }
}
