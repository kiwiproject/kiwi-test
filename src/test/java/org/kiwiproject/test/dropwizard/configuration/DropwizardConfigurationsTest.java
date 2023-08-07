package org.kiwiproject.test.dropwizard.configuration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.kiwiproject.collect.KiwiLists.first;
import static org.kiwiproject.collect.KiwiLists.second;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.configuration.ConfigurationValidationException;
import io.dropwizard.core.Configuration;
import io.dropwizard.testing.ResourceHelpers;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@DisplayName("DropwizardConfigurations")
@ExtendWith(SoftAssertionsExtension.class)
class DropwizardConfigurationsTest {

    private static final Path PARENT_DIR = Paths.get("dropwizard-configuration");
    private static final String INVALID_CONFIG = "invalid-dropwizard-config.yml";
    private static final String VALID_CONFIG = "valid-dropwizard-config.yml";

    @Nested
    class NewConfigurationWithPath {

        @Test
        void shouldThrowExceptionWhenUnknownPath() {
            // the factory doesn't know it's in a subdirectory
            var path = Path.of(VALID_CONFIG);

            assertThatThrownBy(() ->
                    DropwizardConfigurations.newConfiguration(TestConfig.class, path))
                    .isExactlyInstanceOf(DropwizardConfigurations.ConfigurationFactoryException.class)
                    .hasMessage("Unable to locate %s in either the root path or in src/test/resources", path);
        }

        @Test
        void shouldFailValidationWhenInvalidConfiguration() {
            var path = PARENT_DIR.resolve(INVALID_CONFIG);

            assertThatThrownBy(() ->
                    DropwizardConfigurations.newConfiguration(TestConfig.class, path))
                    .isExactlyInstanceOf(DropwizardConfigurations.ConfigurationFactoryException.class)
                    .hasCauseExactlyInstanceOf(ConfigurationValidationException.class)
                    .hasMessage(
                            "Problem occurred while factory was trying to create config from src/test/resources/%s",
                            path);
        }

        @Test
        void shouldReturnValidConfiguration(SoftAssertions softly) {
            var path = PARENT_DIR.resolve(VALID_CONFIG);
            var testConfig = DropwizardConfigurations.newConfiguration(TestConfig.class, path);

            var people = testConfig.getPeople();
            assertThatExpectedPeopleArePresent(softly, people);
        }

        @Test
        void shouldReturnConfigurationWhenGivenAbsolutePath(SoftAssertions softly) {
            var path = Path.of(ResourceHelpers.resourceFilePath("dropwizard-configuration/valid-dropwizard-config.yml"));
            var testConfig = DropwizardConfigurations.newConfiguration(TestConfig.class, path);

            var people = testConfig.getPeople();
            assertThatExpectedPeopleArePresent(softly, people);
        }
    }

    @Nested
    class NewConfigurationWithString {

        @Test
        void shouldThrowExceptionWhenUnknownPath() {
            // the factory doesn't know it's in a subdirectory
            var path = Path.of(VALID_CONFIG).toString();

            assertThatThrownBy(() ->
                    DropwizardConfigurations.newConfiguration(TestConfig.class, VALID_CONFIG))
                    .isExactlyInstanceOf(DropwizardConfigurations.ConfigurationFactoryException.class)
                    .hasMessage("Unable to locate %s in either the root path or in src/test/resources", path);
        }

        @Test
        void shouldFailValidationWhenInvalidConfiguration() {
            var path = PARENT_DIR.resolve(INVALID_CONFIG).toString();

            assertThatThrownBy(() ->
                    DropwizardConfigurations.newConfiguration(TestConfig.class, path))
                    .isExactlyInstanceOf(DropwizardConfigurations.ConfigurationFactoryException.class)
                    .hasCauseExactlyInstanceOf(ConfigurationValidationException.class)
                    .hasMessage(
                            "Problem occurred while factory was trying to create config from src/test/resources/%s",
                            path);
        }

        @Test
        void shouldReturnValidConfiguration(SoftAssertions softly) {
            var path = PARENT_DIR.resolve(VALID_CONFIG).toString();
            var testConfig = DropwizardConfigurations.newConfiguration(TestConfig.class, path);

            var people = testConfig.getPeople();
            assertThatExpectedPeopleArePresent(softly, people);
        }
    }

    private void assertThatExpectedPeopleArePresent(SoftAssertions softly, List<Person> people) {
        var firstPerson = first(people);
        softly.assertThat(firstPerson.getName()).isEqualTo("Alice");
        softly.assertThat(firstPerson.getAge()).isEqualTo(29);

        var secondPerson = second(people);
        softly.assertThat(secondPerson.getName()).isEqualTo("Bob");
        softly.assertThat(secondPerson.getAge()).isEqualTo(42);
    }

    @Getter
    @Setter
    static final class TestConfig extends Configuration {

        @JsonProperty("people")
        @Valid
        @NotNull
        List<Person> people;
    }

    @Getter
    @Setter
    static final class Person {

        @NotBlank
        private String name;

        @NotNull
        @Min(0)
        @Max(120)
        private Integer age;
    }
}
