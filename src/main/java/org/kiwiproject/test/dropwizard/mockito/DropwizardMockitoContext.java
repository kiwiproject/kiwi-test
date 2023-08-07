package org.kiwiproject.test.dropwizard.mockito;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.core.setup.AdminEnvironment;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import jakarta.validation.Validator;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Contains all the various top-level objects in a Dropwizard application.
 * <p>
 * The contained objects are specifically intended to be Mockito mocks, but this is not checked in any way.
 */
@Getter
@Accessors(fluent = true)
@Builder(access = AccessLevel.PACKAGE)
public class DropwizardMockitoContext {
    private final Environment environment;
    private final AdminEnvironment adminEnvironment;
    private final JerseyEnvironment jersey;
    private final HealthCheckRegistry healthChecks;
    private final MetricRegistry metrics;
    private final LifecycleEnvironment lifecycle;
    private final ObjectMapper objectMapper;
    private final Validator validator;
}
