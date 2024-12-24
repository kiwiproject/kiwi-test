package org.kiwiproject.test.dropwizard.app;

import static java.util.stream.Collectors.toUnmodifiableSet;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import io.dropwizard.core.Configuration;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.lifecycle.JettyManaged;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.lifecycle.ServerLifecycleListener;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import lombok.experimental.UtilityClass;
import org.eclipse.jetty.util.component.LifeCycle;
import org.glassfish.jersey.internal.inject.InstanceBinding;
import org.kiwiproject.reflect.KiwiReflection;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Test utility for testing Dropwizard apps when using {@link DropwizardAppExtension}.
 */
@UtilityClass
public class DropwizardAppTests {

    /**
     * Dropwizard does not expose this as a public class, so we have to resort to reflection and hackery to get at
     * this class. It is, unfortunately, the class used when you register a {@link ServerLifecycleListener} in
     * a Dropwizard application, so we need to access it somehow.
     */
    @VisibleForTesting
    static final String DROPWIZARD_PRIVATE_SERVER_LISTENER_CLASS_NAME =
            "io.dropwizard.lifecycle.setup.LifecycleEnvironment$ServerListener";

    // Resources

    /**
     * Find the resource classes registered in the given Dropwizard app.
     *
     * @param app the DropwizardAppExtension containing the Dropwizard app being tested
     * @param <C> the configuration type
     * @return list containing registered resource types
     */
    public static <C extends Configuration> List<Class<?>> registeredResourceClassesOf(DropwizardAppExtension<C> app) {
        return registeredResourceClassesOf(app.getEnvironment().jersey());
    }

    /**
     * Find the resource classes registered in the given {@link JerseyEnvironment}.
     *
     * @param jersey the {@link JerseyEnvironment} associated with the Dropwizard app being tested
     * @return list containing registered resource types
     */
    public static List<Class<?>> registeredResourceClassesOf(JerseyEnvironment jersey) {
        return registeredResourceObjectsOf(jersey)
                .stream()
                .<Class<?>>map(Object::getClass)
                .toList();
    }

    /**
     * Find the resource objects registered in the given Dropwizard app.
     *
     * @param app the DropwizardAppExtension containing the Dropwizard app being tested
     * @param <C> the configuration type
     * @return set containing registered resource objects
     */
    public static <C extends Configuration> Set<Object> registeredResourceObjectsOf(DropwizardAppExtension<C> app) {
        return registeredResourceObjectsOf(app.getEnvironment().jersey());
    }

    /**
     * Find the resource objects registered in the given {@link JerseyEnvironment}.
     *
     * @param jersey the {@link JerseyEnvironment} associated with the Dropwizard app being tested
     * @return set containing registered resource objects
     * @implNote Dropwizard 2.0 added one more layer of indirection for resource endpoint classes; they are now
     * wrapped inside a {@link DropwizardResourceConfig.SpecificBinder}, so this method needs to unwrap those and
     * add the wrapped objects to the returned set. To achieve that, it has to perform some nastiness with the
     * {@link org.glassfish.jersey.internal.inject.Binding} returned by
     * {@link DropwizardResourceConfig.SpecificBinder#getBindings()}, specifically it must cast to
     * {@link InstanceBinding} to be able to get the "service" which is the actual object we want. This is
     * <em>extremely brittle</em> and we're only leaving it like this because this is a test helper class. Note also
     * that the set returned by this class contains more objects that is returned by
     * {@link DropwizardResourceConfig#getInstances()} since we do not remove the wrapped classes.
     */
    public static Set<Object> registeredResourceObjectsOf(JerseyEnvironment jersey) {
        var instances = jersey.getResourceConfig().getInstances();

        var wrappedResourceObjects = instances.stream()
                .filter(o -> o instanceof DropwizardResourceConfig.SpecificBinder)
                .map(DropwizardResourceConfig.SpecificBinder.class::cast)
                .flatMap(specificBinder -> specificBinder.getBindings().stream())
                .filter(InstanceBinding.class::isInstance)
                .map(InstanceBinding.class::cast)
                .map(InstanceBinding::getService)
                .collect(toUnmodifiableSet());

        return Sets.union(instances, wrappedResourceObjects);
    }

    // Health checks

    /**
     * Find the health check names registered in the given Dropwizard app.
     *
     * @param app the DropwizardAppExtension containing the Dropwizard app being tested
     * @param <C> the configuration type
     * @return a set containing the health check names
     */
    public static <C extends Configuration> SortedSet<String> healthCheckNamesOf(DropwizardAppExtension<C> app) {
        return app.getEnvironment().healthChecks().getNames();
    }

    // Lifecycle & Managed

    /**
     * Finds the {@link Managed} objects registered in the given Dropwizard app.
     *
     * @param app the DropwizardAppExtension containing the Dropwizard app being tested
     * @param <C> the configuration type
     * @return list of Managed objects
     */
    public static <C extends Configuration> List<Managed> managedObjectsOf(DropwizardAppExtension<C> app) {
        return managedObjectsOf(app.getEnvironment().lifecycle());
    }

    /**
     * Finds the {@link Managed} objects registered in the given {@link LifecycleEnvironment}.
     *
     * @param lifecycle the {@link LifecycleEnvironment} associated with the Dropwizard app being tested
     * @return list of Managed objects
     */
    public static List<Managed> managedObjectsOf(LifecycleEnvironment lifecycle) {
        return managedObjectStreamOf(lifecycle).toList();
    }

    /**
     * Finds {@link Managed} objects registered in the given Dropwizard app and having the given type.
     *
     * @param app  the DropwizardAppExtension containing the Dropwizard app being tested
     * @param type the type of object to find
     * @param <C>  the configuration type
     * @param <T>  the object type
     * @return list of managed objects
     */
    public static <C extends Configuration, T> List<T> managedObjectsOfType(DropwizardAppExtension<C> app,
                                                                            Class<T> type) {
        return managedObjectsOfType(app.getEnvironment().lifecycle(), type);
    }

    /**
     * Finds {@link Managed} objects registered in the given {@link LifecycleEnvironment} and having the given type.
     *
     * @param lifecycle the {@link LifecycleEnvironment} associated with the Dropwizard app being tested
     * @param type      the type of object to find
     * @param <T>       the object type
     * @return list of managed objects of type T
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> managedObjectsOfType(LifecycleEnvironment lifecycle, Class<T> type) {
        return (List<T>) managedObjectsOf(lifecycle, managed -> type.isAssignableFrom(managed.getClass()));
    }

    /**
     * Finds {@link Managed} objects registered in the given Dropwizard app that match the given predicate.
     *
     * @param app       the DropwizardAppExtension containing the Dropwizard app being tested
     * @param predicate the predicate to match
     * @param <C>       the configuration type
     * @return list of Managed objects
     */
    public static <C extends Configuration> List<Managed> managedObjectsOf(DropwizardAppExtension<C> app,
                                                                           Predicate<Managed> predicate) {
        return managedObjectsOf(app.getEnvironment().lifecycle(), predicate);
    }

    /**
     * Finds {@link Managed} objects registered in the given {@link LifecycleEnvironment} that match the
     * given predicate.
     *
     * @param lifecycle the {@link LifecycleEnvironment} associated with the Dropwizard app being tested
     * @param predicate the predicate to match
     * @return list of Managed objects
     */
    public static List<Managed> managedObjectsOf(LifecycleEnvironment lifecycle, Predicate<Managed> predicate) {
        return managedObjectStreamOf(lifecycle)
                .filter(predicate)
                .toList();
    }

    /**
     * Finds the first {@link Managed} object registered in the given Dropwizard app having the given type.
     *
     * @param app  the DropwizardAppExtension containing the Dropwizard app being tested
     * @param type the type of object to find
     * @param <C>  the configuration type
     * @param <T>  the object type
     * @return optional that might contain the first Managed that is found
     */
    public static <C extends Configuration, T> Optional<T> firstManagedObjectOfType(DropwizardAppExtension<C> app,
                                                                                    Class<T> type) {
        return firstManagedObjectOfType(app.getEnvironment().lifecycle(), type);
    }

    /**
     * Finds the first {@link Managed} object registered in the given {@link LifecycleEnvironment} having the
     * given type.
     *
     * @param lifecycle the {@link LifecycleEnvironment} associated with the Dropwizard app being tested
     * @param type      the type of object to find
     * @param <T>       the object type
     * @return optional that might contain the first Managed that is found
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> firstManagedObjectOfType(LifecycleEnvironment lifecycle, Class<T> type) {
        return (Optional<T>) firstManagedObject(lifecycle, managed -> type.isAssignableFrom(managed.getClass()));
    }

    /**
     * Finds the first {@link Managed} object registered in the given Dropwizard app matching the given predicate.
     *
     * @param app       the DropwizardAppExtension containing the Dropwizard app being tested
     * @param predicate the predicate to match
     * @param <C>       the configuration type
     * @return optional that might contain the first Managed that is found
     */
    public static <C extends Configuration> Optional<Managed> firstManagedObject(DropwizardAppExtension<C> app,
                                                                                 Predicate<Managed> predicate) {
        return firstManagedObject(app.getEnvironment().lifecycle(), predicate);
    }

    /**
     * Finds the first {@link Managed} object registered in the given {@link LifecycleEnvironment} matching the
     * given predicate.
     *
     * @param lifecycle the {@link LifecycleEnvironment} associated with the Dropwizard app being tested
     * @param predicate the predicate to match
     * @return optional that might contain the first Managed that is found
     */
    public static Optional<Managed> firstManagedObject(LifecycleEnvironment lifecycle,
                                                       Predicate<Managed> predicate) {
        return managedObjectStreamOf(lifecycle)
                .filter(predicate)
                .findFirst();
    }

    /**
     * Streams {@link Managed} objects registered in the given Dropwizard app.
     *
     * @param app the DropwizardAppExtension containing the Dropwizard app being tested
     * @param <C> the configuration type
     * @return stream of Managed objects
     */
    public static <C extends Configuration> Stream<Managed> managedObjectStreamOf(DropwizardAppExtension<C> app) {
        return managedObjectStreamOf(app.getEnvironment().lifecycle());
    }

    /**
     * Streams {@link Managed} objects registered in the given {@link LifecycleEnvironment}.
     *
     * @param lifecycle the {@link LifecycleEnvironment} associated with the Dropwizard app being tested
     * @return stream of Managed objects
     */
    public static Stream<Managed> managedObjectStreamOf(LifecycleEnvironment lifecycle) {
        return lifeCycleStreamOf(lifecycle)
                .map(JettyManaged.class::cast)
                .map(JettyManaged::getManaged);
    }

    /**
     * Finds {@link LifeCycle} objects registered in the given Dropwizard app.
     *
     * @param app the DropwizardAppExtension containing the Dropwizard app being tested
     * @param <C> the configuration type
     * @return list of LifeCycle objects
     */
    public static <C extends Configuration> List<LifeCycle> lifeCycleObjectsOf(DropwizardAppExtension<C> app) {
        return lifeCycleObjectsOf(app.getEnvironment().lifecycle());
    }

    /**
     * Finds {@link LifeCycle} objects registered in the given {@link LifecycleEnvironment}.
     *
     * @param lifecycle the {@link LifecycleEnvironment} associated with the Dropwizard app being tested
     * @return list of LifeCycle objects
     */
    public static List<LifeCycle> lifeCycleObjectsOf(LifecycleEnvironment lifecycle) {
        return lifecycle.getManagedObjects();
    }

    /**
     * Streams {@link LifeCycle} objects registered in the given Dropwizard app.
     *
     * @param app the DropwizardAppExtension containing the Dropwizard app being tested
     * @param <C> the configuration type
     * @return stream of LifeCycle objects
     */
    public static <C extends Configuration> Stream<LifeCycle> lifeCycleStreamOf(DropwizardAppExtension<C> app) {
        return lifeCycleStreamOf(app.getEnvironment().lifecycle());
    }

    /**
     * Streams {@link LifeCycle} objects registered in the given {@link LifecycleEnvironment}.
     *
     * @param lifecycle the {@link LifecycleEnvironment} associated with the Dropwizard app being tested
     * @return stream of LifeCycle objects
     */
    public static Stream<LifeCycle> lifeCycleStreamOf(LifecycleEnvironment lifecycle) {
        return lifecycle.getManagedObjects().stream();
    }

    /**
     * Finds {@link LifeCycle.Listener} classes registered with the given Dropwizard app.
     *
     * @param app the DropwizardAppExtension containing the Dropwizard app being tested
     * @param <C> the configuration type
     * @return list of lifecycle listener classes
     */
    public static <C extends Configuration> List<Class<?>> lifeCycleListenerClassesOf(
            DropwizardAppExtension<C> app) {

        return lifeCycleListenerClassesOf(app.getEnvironment().lifecycle());
    }

    /**
     * Finds {@link LifeCycle.Listener} classes registered with the given {@link LifecycleEnvironment}.
     *
     * @param lifecycle the {@link LifecycleEnvironment} associated with the Dropwizard app being tested
     * @return list of lifecycle listener classes
     */
    public static List<Class<?>> lifeCycleListenerClassesOf(LifecycleEnvironment lifecycle) {
        return lifeCycleListenersOf(lifecycle)
                .stream()
                .<Class<?>>map(Object::getClass)
                .toList();
    }

    /**
     * Finds {@link LifeCycle.Listener} objects registered with the given Dropwizard app.
     *
     * @param app the DropwizardAppExtension containing the Dropwizard app being tested
     * @param <C> the configuration type
     * @return list of lifecycle listener objects
     */
    public static <C extends Configuration> List<LifeCycle.Listener> lifeCycleListenersOf(
            DropwizardAppExtension<C> app) {

        return lifeCycleListenersOf(app.getEnvironment().lifecycle());
    }

    /**
     * Finds {@link LifeCycle.Listener} objects registered with the given {@link LifecycleEnvironment}.
     *
     * @param lifecycle the {@link LifecycleEnvironment} associated with the Dropwizard app being tested
     * @return list of lifecycle listener objects
     */
    @SuppressWarnings("unchecked")
    public static List<LifeCycle.Listener> lifeCycleListenersOf(LifecycleEnvironment lifecycle) {
        return (List<LifeCycle.Listener>) KiwiReflection.getFieldValue(lifecycle, "lifecycleListeners");
    }

    /**
     * Finds {@link ServerLifecycleListener} objects registered with the given Dropwizard app.
     *
     * @param app the DropwizardAppExtension containing the Dropwizard app being tested
     * @param <C> the configuration type
     * @return list of server lifecycle listener objects
     */
    public static <C extends Configuration> List<ServerLifecycleListener> serverLifecycleListenersOf(
            DropwizardAppExtension<C> app) {

        return serverLifecycleListenersOf(app.getEnvironment().lifecycle());
    }

    /**
     * Finds {@link ServerLifecycleListener} objects registered with the given {@link LifecycleEnvironment}.
     *
     * @param lifecycle the {@link LifecycleEnvironment} associated with the Dropwizard app being tested
     * @return list of server lifecycle listener objects
     */
    public static List<ServerLifecycleListener> serverLifecycleListenersOf(LifecycleEnvironment lifecycle) {
        var serverListeners = lifeCycleListenersOf(lifecycle).stream()
                .filter(listener ->
                        listener.getClass().getName().equals(DROPWIZARD_PRIVATE_SERVER_LISTENER_CLASS_NAME))
                .toList();

        return serverListeners.stream()
                .map(listener -> KiwiReflection.getFieldValue(listener, "listener"))
                .map(ServerLifecycleListener.class::cast)
                .toList();
    }
}
