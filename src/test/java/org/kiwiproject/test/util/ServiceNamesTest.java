package org.kiwiproject.test.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.dropwizard.util.Resources;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;

@DisplayName("ServiceNames")
class ServiceNamesTest {

    private static String basePath;

    @BeforeAll
    static void beforeAll() throws URISyntaxException {
        var uri = Resources.getResource("ServiceNamesTest").toURI();
        var file = new File(uri);
        basePath = file.getAbsolutePath();
    }

    @Nested
    class FindServiceOrEmulatorNameFromRoot {

        @Test
        void shouldFindService_InTopLevelPom() {
            var rootPath = Path.of(basePath, "top-level-service").toString();
            var name = ServiceNames.findServiceOrEmulatorNameFromRoot(rootPath);
            assertThat(name).isEqualTo("kiwi-test-sample-service");
        }

        @Test
        void shouldFindEmulator_InTopLevelPom() {
            var rootPath = Path.of(basePath, "top-level-emulator").toString();
            var name = ServiceNames.findServiceOrEmulatorNameFromRoot(rootPath);
            assertThat(name).isEqualTo("kiwi-test-sample-emulator");
        }

        @Test
        void shouldFindService_InNestedDirectory() {
            var rootPath = Path.of(basePath, "nested-service").toString();
            var name = ServiceNames.findServiceOrEmulatorNameFromRoot(rootPath);
            assertThat(name).isEqualTo("kiwi-test-sample-nested-service");
        }

        @Test
        void shouldThrow_WhenPomNotFound() {
            var rootPath = Path.of(basePath).toString();
            assertThatThrownBy(() -> ServiceNames.findServiceOrEmulatorNameFromRoot(rootPath))
                    .isExactlyInstanceOf(IllegalStateException.class)
                    .hasMessage("Root pom.xml does not exist! Path: " + Path.of(rootPath, "pom.xml"));
        }

        @Test
        void shouldThrow_WhenNotServiceOrEmulator() {
            var rootPath = Path.of(basePath, "top-level-app").toString();
            assertThatThrownBy(() -> ServiceNames.findServiceOrEmulatorNameFromRoot(rootPath))
                    .isExactlyInstanceOf(IllegalStateException.class)
                    .hasMessageStartingWith("kiwi-test-sample-app")
                    .hasMessageContaining("<artifactId>");
        }

        @Test
        void shouldThrow_WhenNoParentPom() {
            var rootPath = Path.of(basePath, "no-parent-pom").toString();
            assertThatThrownBy(() -> ServiceNames.findServiceOrEmulatorNameFromRoot(rootPath))
                    .isExactlyInstanceOf(IllegalStateException.class)
                    .hasMessage("Did not find <parent> tag in POM!");
        }
    }

    @Nested
    class IsServiceOrEmulator {

        @ParameterizedTest
        @ValueSource(strings = {
                "foo-service",
                "bar-service",
                "baz-emulator",
                "corge-service",
                "quux-emulator"
        })
        void shouldReturnTrue_ForServicesAndEmulators(String name) {
            assertThat(ServiceNames.isServiceOrEmulator(name)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "foo-app",
                "bar-service-app",
                "baz-emulator-app",
                "foo",
                "other",
                "12345"
        })
        void shouldReturnFalse_ForOtherNames(String name) {
            assertThat(ServiceNames.isServiceOrEmulator(name)).isFalse();
        }
    }

    @Nested
    class AssertIsServiceOrEmulator {

        @ParameterizedTest
        @ValueSource(strings = {
                "foo-service",
                "bar-service",
                "baz-emulator",
                "corge-service",
                "quux-emulator"
        })
        void shouldNotThrow_ForServicesAndEmulators(String name) {
            assertThatCode(() -> ServiceNames.assertIsServiceOrEmulator(name)).doesNotThrowAnyException();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "foo-app",
                "bar-service-app",
                "baz-emulator-app",
                "foo",
                "other",
                "12345"
        })
        void shouldThrow_ForOtherNames(String name) {
            assertThatThrownBy(() -> ServiceNames.assertIsServiceOrEmulator(name))
                    .isExactlyInstanceOf(IllegalStateException.class)
                    .hasMessage(name + " does not end with -service or -emulator");
        }
    }
}