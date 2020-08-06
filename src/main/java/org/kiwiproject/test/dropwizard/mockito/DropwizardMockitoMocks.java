package org.kiwiproject.test.dropwizard.mockito;

import static org.kiwiproject.base.KiwiPreconditions.checkArgumentNotNull;
import static org.kiwiproject.test.constants.KiwiTestConstants.OBJECT_MAPPER;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Verify;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import io.dropwizard.setup.Environment;
import lombok.experimental.UtilityClass;
import org.kiwiproject.test.validation.ValidationTestHelper;
import org.mockito.Mockito;

import javax.validation.Validator;

/**
 * Creates Mockito mocks of Dropwizard application level objects.
 * <p>
 * This should be used rarely and only in specific situations where you might not want to use the
 * {@link io.dropwizard.testing.junit5.DropwizardAppExtension} to test a Dropwizard application.
 */
@UtilityClass
public class DropwizardMockitoMocks {

    /**
     * Mock the Dropwizard application environment.
     * <p>
     * All objects returned by the mock {@link Environment} are themselves mock objects.
     *
     * @return a mock {@link Environment} that returns mock Dropwizard application objects
     */
    public static Environment mockEnvironment() {
        return mockEnvironment(OBJECT_MAPPER, ValidationTestHelper.getValidator());
    }

    /**
     * Mock the Dropwizard application environment.
     * <p>
     * All objects returned by the mock {@link Environment} are themselves mock objects.
     *
     * @param mapper    the {@link ObjectMapper} that the mocked environment should return
     * @param validator the {@link Validator} that the mocked environment should return
     * @return a mock {@link Environment} that returns mock Dropwizard application objects
     */
    public static Environment mockEnvironment(ObjectMapper mapper, Validator validator) {
        var env = mock(Environment.class);
        mockJerseyEnvironment(env);
        mockHealthCheckRegistry(env);
        mockMetricRegistry(env);
        mockLifecycleEnvironment(env);
        useObjectMapper(env, mapper);
        useValidator(env, validator);
        return env;
    }

    /**
     * Mock the Dropwizard application environment.
     * <p>
     * All objects contained in the {@link DropwizardMockitoContext} are mocks.
     *
     * @return a context object providing easy access to the mocked Dropwizard application objects
     */
    public static DropwizardMockitoContext mockDropwizardEnvironment() {
        return mockDropwizardEnvironment(OBJECT_MAPPER, ValidationTestHelper.getValidator());
    }

    /**
     * Mock the Dropwizard application environment.
     * <p>
     * All objects contained in the {@link DropwizardMockitoContext} are mocks.
     *
     * @param mapper    the {@link ObjectMapper} that the mocked environment should return
     * @param validator the {@link Validator} that the mocked environment should return
     * @return a context object providing easy access to the mocked Dropwizard application objects
     */
    public static DropwizardMockitoContext mockDropwizardEnvironment(ObjectMapper mapper, Validator validator) {
        var env = mock(Environment.class);
        useObjectMapper(env, mapper);
        useValidator(env, validator);

        return DropwizardMockitoContext.builder()
                .environment(env)
                .jersey(mockJerseyEnvironment(env))
                .healthChecks(mockHealthCheckRegistry(env))
                .metrics(mockMetricRegistry(env))
                .lifecycle(mockLifecycleEnvironment(env))
                .objectMapper(mapper)
                .validator(validator)
                .build();
    }

    /**
     * Mock the {@link JerseyEnvironment} in the given Dropwizard environment.
     *
     * @param mockEnv a mock {@link Environment}
     * @return a mock {@link JerseyEnvironment}
     */
    public static JerseyEnvironment mockJerseyEnvironment(Environment mockEnv) {
        verifyIsMockitoMock(mockEnv);
        var jersey = mock(JerseyEnvironment.class);
        when(mockEnv.jersey()).thenReturn(jersey);
        return jersey;
    }

    /**
     * Mock the {@link HealthCheckRegistry} in the given Dropwizard environment.
     *
     * @param mockEnv a mock {@link Environment}
     * @return a mock {@link HealthCheckRegistry}
     */
    public static HealthCheckRegistry mockHealthCheckRegistry(Environment mockEnv) {
        verifyIsMockitoMock(mockEnv);
        var healthCheckRegistry = mock(HealthCheckRegistry.class);
        when(mockEnv.healthChecks()).thenReturn(healthCheckRegistry);
        return healthCheckRegistry;
    }

    /**
     * Mock the {@link MetricRegistry} in the given Dropwizard environment.
     *
     * @param mockEnv a mock {@link Environment}
     * @return a mock {@link MetricRegistry}
     */
    public static MetricRegistry mockMetricRegistry(Environment mockEnv) {
        verifyIsMockitoMock(mockEnv);
        var metricRegistry = mock(MetricRegistry.class);
        when(mockEnv.metrics()).thenReturn(metricRegistry);
        return metricRegistry;
    }

    /**
     * Mock the {@link LifecycleEnvironment} in the given Dropwizard environment.
     *
     * @param mockEnv a mock {@link Environment}
     * @return a mock {@link LifecycleEnvironment}
     */
    public static LifecycleEnvironment mockLifecycleEnvironment(Environment mockEnv) {
        verifyIsMockitoMock(mockEnv);
        var lifecycleEnvironment = mock(LifecycleEnvironment.class);
        when(mockEnv.lifecycle()).thenReturn(lifecycleEnvironment);
        return lifecycleEnvironment;
    }

    /**
     * Mock the given Dropwizard environment to return the given {@link ObjectMapper}.
     *
     * @param mockEnv a mock {@link Environment}
     * @param mapper  the mapper that the mocked environment should return
     */
    public static void useObjectMapper(Environment mockEnv, ObjectMapper mapper) {
        verifyIsMockitoMock(mockEnv);
        checkArgumentNotNull(mapper);
        when(mockEnv.getObjectMapper()).thenReturn(mapper);
    }

    /**
     * Mock the given Dropwizard environment to return the given {@link Validator}.
     *
     * @param mockEnv   a mock {@link Environment}
     * @param validator the validator that the mocked environment should return
     */
    public static void useValidator(Environment mockEnv, Validator validator) {
        verifyIsMockitoMock(mockEnv);
        checkArgumentNotNull(validator);
        when(mockEnv.getValidator()).thenReturn(validator);
    }

    private static void verifyIsMockitoMock(Environment environment) {
        checkArgumentNotNull(environment);
        Verify.verify(Mockito.mockingDetails(environment).isMock(), "environment must be a Mockito mock");
    }
}
