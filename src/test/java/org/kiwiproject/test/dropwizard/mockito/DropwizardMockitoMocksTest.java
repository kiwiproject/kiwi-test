package org.kiwiproject.test.dropwizard.mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.kiwiproject.test.constants.KiwiTestConstants.OBJECT_MAPPER;
import static org.mockito.Mockito.mock;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.common.base.VerifyException;
import io.dropwizard.Configuration;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.setup.Environment;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.kiwiproject.test.validation.ValidationTestHelper;
import org.mockito.Mockito;

import javax.validation.Validation;

@DisplayName("DropwizardMockitoMocks")
class DropwizardMockitoMocksTest {

    @Nested
    class MockEnvironment {

        private Environment env;

        @BeforeEach
        void setUp() {
            env = DropwizardMockitoMocks.mockEnvironment();
        }

        @Test
        void shouldReturnEnvironment() {
            assertIsMockitoMock(env);
        }

        @Test
        void shouldSetJerseyEnvironment() {
            assertIsMockitoMock(env.jersey());
        }

        @Test
        void shouldSetHealthCheckRegistry() {
            assertIsMockitoMock(env.healthChecks());
        }

        @Test
        void shouldSetMetricRegistry() {
            assertIsMockitoMock(env.metrics());
        }

        @Test
        void shouldSetLifecycleEnvironment() {
            assertIsMockitoMock(env.lifecycle());
        }

        @Test
        void shouldSetObjectMapper() {
            assertThat(env.getObjectMapper()).isSameAs(OBJECT_MAPPER);
        }

        @Test
        void shouldSetValidator() {
            assertThat(env.getValidator()).isSameAs(ValidationTestHelper.getValidator());
        }
    }

    @Nested
    class MockDropwizardEnvironment {

        private DropwizardMockitoContext context;

        @BeforeEach
        void setUp() {
            context = DropwizardMockitoMocks.mockDropwizardEnvironment();
        }

        @Test
        void shouldSetEnvironment() {
            assertIsMockitoMock(context.environment());
        }

        @Test
        void shouldSetJerseyEnvironment() {
            var jersey = context.jersey();
            assertIsMockitoMock(jersey);
            assertThat(context.environment().jersey()).isSameAs(jersey);
        }

        @Test
        void shouldSetHealthCheckRegistry() {
            var healthChecks = context.healthChecks();
            assertIsMockitoMock(healthChecks);
            assertThat(context.environment().healthChecks()).isSameAs(healthChecks);
        }

        @Test
        void shouldSetMetricRegistry() {
            var metrics = context.metrics();
            assertIsMockitoMock(metrics);
            assertThat(context.environment().metrics()).isSameAs(metrics);
        }

        @Test
        void shouldSetLifecycleEnvironment() {
            var lifecycle = context.lifecycle();
            assertIsMockitoMock(lifecycle);
            assertThat(context.environment().lifecycle()).isSameAs(lifecycle);
        }

        @Test
        void shouldSetObjectMapper() {
            var mapper = context.objectMapper();
            assertThat(mapper).isSameAs(OBJECT_MAPPER);
            assertThat(context.environment().getObjectMapper()).isSameAs(mapper);
        }

        @Test
        void shouldSetValidator() {
            var validator = context.validator();
            assertThat(validator).isSameAs(ValidationTestHelper.getValidator());
            assertThat(context.environment().getValidator()).isSameAs(validator);
        }
    }

    @Nested
    class MethodsRequiringMockEnvironment {

        private Environment env;

        @BeforeEach
        void setUp() {
            env = mock(Environment.class);
        }

        @Test
        void shouldRequireMockitoMocks() {
            var realEnv = new Environment(
                    "test",
                    OBJECT_MAPPER,
                    Validation.buildDefaultValidatorFactory(),
                    new MetricRegistry(),
                    Thread.currentThread().getContextClassLoader(),
                    new HealthCheckRegistry(),
                    new TestConfig()
            );

            SoftAssertions.assertSoftly(softly -> {
                assertVerifyExceptionThrownBy(softly, () -> DropwizardMockitoMocks.mockJerseyEnvironment(realEnv));
                assertVerifyExceptionThrownBy(softly, () -> DropwizardMockitoMocks.mockHealthCheckRegistry(realEnv));
                assertVerifyExceptionThrownBy(softly, () -> DropwizardMockitoMocks.mockMetricRegistry(realEnv));
                assertVerifyExceptionThrownBy(softly, () -> DropwizardMockitoMocks.mockLifecycleEnvironment(realEnv));
                assertVerifyExceptionThrownBy(softly,
                        () -> DropwizardMockitoMocks.useObjectMapper(realEnv, OBJECT_MAPPER));
                assertVerifyExceptionThrownBy(softly,
                        () -> DropwizardMockitoMocks.useValidator(realEnv, ValidationTestHelper.getValidator()));
            });
        }

        private void assertVerifyExceptionThrownBy(SoftAssertions softly,
                                                   ThrowableAssert.ThrowingCallable callable) {
            softly.assertThatThrownBy(callable)
                    .isExactlyInstanceOf(VerifyException.class)
                    .hasMessage("environment must be a Mockito mock");
        }

        @Test
        void shouldMockJerseyEnvironment() {
            var jersey = DropwizardMockitoMocks.mockJerseyEnvironment(env);
            assertIsMockitoMock(jersey);
            assertThat(env.jersey()).isSameAs(jersey);
        }

        @Test
        void shouldMockHealthCheckRegistry() {
            var healthChecks = DropwizardMockitoMocks.mockHealthCheckRegistry(env);
            assertIsMockitoMock(healthChecks);
            assertThat(env.healthChecks()).isSameAs(healthChecks);
        }

        @Test
        void shouldMockMetricRegistry() {
            var metrics = DropwizardMockitoMocks.mockMetricRegistry(env);
            assertIsMockitoMock(metrics);
            assertThat(env.metrics()).isSameAs(metrics);
        }

        @Test
        void shouldMockLifecycleEnvironment() {
            var lifecycle = DropwizardMockitoMocks.mockLifecycleEnvironment(env);
            assertIsMockitoMock(lifecycle);
            assertThat(env.lifecycle()).isSameAs(lifecycle);
        }

        @Test
        void shouldUseObjectMapper() {
            var mapper = Jackson.newMinimalObjectMapper();
            DropwizardMockitoMocks.useObjectMapper(env, mapper);
            assertThat(env.getObjectMapper()).isSameAs(mapper);
        }

        @Test
        void shouldUseValidator() {
            var validator = ValidationTestHelper.newValidator();
            DropwizardMockitoMocks.useValidator(env, validator);
            assertThat(env.getValidator()).isSameAs(validator);
        }
    }

    private static void assertIsMockitoMock(Object object) {
        assertThat(object).isNotNull();
        assertThat(Mockito.mockingDetails(object).isMock()).isTrue();
    }

    static class TestConfig extends Configuration {
    }

}