package org.kiwiproject.test.dropwizard.app;

import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.kiwiproject.collect.KiwiLists.first;

import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.core.Application;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.lifecycle.JettyManaged;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.lifecycle.ServerLifecycleListener;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.component.LifeCycle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kiwiproject.test.junit.jupiter.DropwizardExtensionsSupportWithLoggingReset;

import java.util.List;

@DisplayName("DropwizardAppTests")
@ExtendWith(SoftAssertionsExtension.class)
@DropwizardExtensionsSupportWithLoggingReset
class DropwizardAppTestsTest {

    @Getter
    @Setter
    public static class Config extends Configuration {
        private String tagLine;
        private String answer;
    }

    @AllArgsConstructor
    public static class TagLineResource {

        private final String tagLine;

        @GET
        @Path("/tagline")
        public String tagLine() {
            return tagLine;
        }
    }

    @AllArgsConstructor
    public static class AnswerResource {

        private final String answer;

        @GET
        @Path("/anyQuestion")
        public String anyQuestion() {
            return answer;
        }
    }

    public static class TagLineHealthCheck extends HealthCheck {
        @Override
        protected Result check() {
            return Result.healthy();
        }
    }

    public static class AnswerHealthCheck extends HealthCheck {
        @Override
        protected Result check() {
            return Result.healthy();
        }
    }

    public static class Managed1 extends NoOpManaged {
    }

    public static class Managed2 extends NoOpManaged {
    }

    public static class UnmanagedManaged extends NoOpManaged {
    }

    public static class MyLifeCycleListener implements LifeCycle.Listener {
    }

    public static class MyServerLifecycleListener implements ServerLifecycleListener {
        @Override
        public void serverStarted(Server server) {
            // no-op
        }
    }

    public static class App extends Application<Config> {
        @Override
        public void run(Config config, Environment environment) {
            environment.jersey().register(new TagLineResource(config.getTagLine()));
            environment.jersey().register(new AnswerResource(config.getAnswer()));

            environment.healthChecks().register("tagLine", new TagLineHealthCheck());
            environment.healthChecks().register("answer", new AnswerHealthCheck());

            environment.lifecycle().manage(new Managed1());
            environment.lifecycle().manage(new Managed2());

            environment.lifecycle().addEventListener(new MyLifeCycleListener());
            environment.lifecycle().addServerLifecycleListener(new MyServerLifecycleListener());
        }
    }

    static final DropwizardAppExtension<Config> APP = new DropwizardAppExtension<>(App.class,
            ResourceHelpers.resourceFilePath("dropwizard-app-unit-tests.yml"));

    @Test
    void shouldReturnRegisteredResourceClasses() {
        assertThat(DropwizardAppTests.registeredResourceClassesOf(APP)).contains(
                AnswerResource.class,
                TagLineResource.class
        );
    }

    @Test
    void shouldReturnRegisteredResourceObjects() {
        assertThat(DropwizardAppTests.registeredResourceObjectsOf(APP))
                .hasAtLeastOneElementOfType(AnswerResource.class)
                .hasAtLeastOneElementOfType(TagLineResource.class);
    }

    @Test
    void shouldReturnHealthCheckNames() {
        assertThat(DropwizardAppTests.healthCheckNamesOf(APP)).contains("answer", "tagLine");
    }

    @Test
    void shouldReturnManagedObjects() {
        assertThat(DropwizardAppTests.managedObjectsOf(APP))
                .hasAtLeastOneElementOfType(Managed1.class)
                .hasAtLeastOneElementOfType(Managed2.class);
    }

    @Test
    void shouldReturnManagedObjectsUsingPredicate() {
        assertThat(DropwizardAppTests.managedObjectsOf(APP, Managed2.class::isInstance))
                .hasAtLeastOneElementOfType(Managed2.class)
                .doesNotHaveAnyElementsOfTypes(Managed1.class);
    }

    @Test
    void shouldReturnStreamOfManagedObjects() {
        assertThat(DropwizardAppTests.managedObjectStreamOf(APP))
                .hasAtLeastOneElementOfType(Managed1.class)
                .hasAtLeastOneElementOfType(Managed2.class);
    }

    @Test
    void shouldReturnManagedObjectsOfSpecificType() {
        var noOpManagedObjects = DropwizardAppTests.managedObjectsOfType(APP, NoOpManaged.class);

        assertThat(noOpManagedObjects)
                .hasAtLeastOneElementOfType(Managed1.class)
                .hasAtLeastOneElementOfType(Managed2.class);
    }

    @Test
    void shouldReturnFirstManagedOfType(SoftAssertions softly) {
        var optionalManaged1 = DropwizardAppTests.firstManagedObjectOfType(APP, Managed1.class);
        softly.assertThat(optionalManaged1).isPresent();

        var optionalManaged2 = DropwizardAppTests.firstManagedObjectOfType(APP, Managed2.class);
        softly.assertThat(optionalManaged2).isPresent();

        var optionalManaged3 = DropwizardAppTests.firstManagedObjectOfType(APP, UnmanagedManaged.class);
        softly.assertThat(optionalManaged3).isEmpty();
    }

    @Test
    void shouldReturnFirstManagedObjectUsingPredicate() {
        var optionalManaged = DropwizardAppTests.firstManagedObject(APP, this::isManaged1Or2);

        assertThat(optionalManaged).isPresent();

        var managed = optionalManaged.orElseThrow();
        assertThat(managed).isInstanceOfAny(Managed1.class, Managed2.class);
    }

    private boolean isManaged1Or2(Managed managed) {
        var managedClass = managed.getClass();
        return Managed1.class.isAssignableFrom(managedClass) || Managed2.class.isAssignableFrom(managedClass);
    }

    @Test
    void shouldReturnLifeCycleObjects() {
        var lifeCycles = DropwizardAppTests.lifeCycleObjectsOf(APP);
        assertLifeCycleObjects(lifeCycles);
    }

    @Test
    void shouldReturnStreamOfLifeCycleObjects() {
        var lifeCycles = DropwizardAppTests.lifeCycleStreamOf(APP).toList();
        assertLifeCycleObjects(lifeCycles);
    }

    private void assertLifeCycleObjects(List<LifeCycle> lifeCycles) {
        assertThat(first(lifeCycles)).isInstanceOf(JettyManaged.class);

        var mischiefManaged = lifeCycles.stream()
                .map(JettyManaged.class::cast)
                .map(JettyManaged::getManaged)
                .filter(managed -> NoOpManaged.class.isAssignableFrom(managed.getClass()))
                .map(NoOpManaged.class::cast)
                .map(NoOpManaged::mischiefManaged)
                .collect(toUnmodifiableSet());

        assertThat(mischiefManaged).containsOnly(true);
    }

    @Test
    void shouldReturnLifeCycleListenerClasses(SoftAssertions softly) {
        var classes = DropwizardAppTests.lifeCycleListenerClassesOf(APP);

        softly.assertThat(classes).contains(MyLifeCycleListener.class);
        softly.assertThat(classes).extracting(Class::getName)
                .contains(DropwizardAppTests.DROPWIZARD_PRIVATE_SERVER_LISTENER_CLASS_NAME);
    }

    @Test
    void shouldReturnLifeCycleListenerObjects(SoftAssertions softly) {
        var listeners = DropwizardAppTests.lifeCycleListenersOf(APP);

        softly.assertThat(listeners).hasAtLeastOneElementOfType(MyLifeCycleListener.class);

        var classNames = listeners.stream()
                .map(Object::getClass)
                .map(Class::getName)
                .toList();

        softly.assertThat(classNames).contains(DropwizardAppTests.DROPWIZARD_PRIVATE_SERVER_LISTENER_CLASS_NAME);
    }

    @Test
    void shouldReturnServerLifecycleListeners() {
        var serverLifecycleListeners = DropwizardAppTests.serverLifecycleListenersOf(APP);

        assertThat(serverLifecycleListeners).hasAtLeastOneElementOfType(MyServerLifecycleListener.class);
    }
}
