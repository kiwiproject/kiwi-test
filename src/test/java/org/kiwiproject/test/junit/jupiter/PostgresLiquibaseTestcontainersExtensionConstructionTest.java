package org.kiwiproject.test.junit.jupiter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.util.SetSystemProperty;
import org.testcontainers.utility.DockerImageName;

import java.util.function.UnaryOperator;

@DisplayName("PostgresLiquibaseTestcontainersExtension (Construction)")
class PostgresLiquibaseTestcontainersExtensionConstructionTest {

    @Test
    void shouldCreateExtension_WithDefaults_UsingOfDefaultsFactoryMethod() {
        var extension = PostgresLiquibaseTestcontainersExtension.ofDefaults();

        assertAll(
                () -> assertThat(extension.getImageName())
                        .isEqualTo(DockerImageName.parse("postgres:18-alpine")),
                () -> assertThat(extension.getChangelogPath())
                        .isEqualTo("migrations.xml")
        );
    }

    @Test
    void shouldUseProvidedImage() {
        var extension = PostgresLiquibaseTestcontainersExtension.builder()
                .imageName(DockerImageName.parse("postgres:18-trixie"))
                .build();

        assertThat(extension.getImageName())
                .isEqualTo(DockerImageName.parse("postgres:18-trixie"));
    }

    // suppress sonar duplicate code warning: same test code as above, except setting a system property
    @SuppressWarnings("java:S4144")
    @Test
    @SetSystemProperty(key = "kiwi.test.postgres.image", value = "postgres:16")
    void shouldPreferExplicitImage_OverSystemProperty() {
        var extension = PostgresLiquibaseTestcontainersExtension.builder()
                .imageName(DockerImageName.parse("postgres:18-trixie"))
                .build();

        assertThat(extension.getImageName())
                .isEqualTo(DockerImageName.parse("postgres:18-trixie"));
    }

    @Test
    @SetSystemProperty(key = "kiwi.test.postgres.image", value = "postgres:16")
    void shouldUseImageFromSystemProperty_WhenImageNotProvided() {
        var extension = PostgresLiquibaseTestcontainersExtension.builder().build();

        assertThat(extension.getImageName())
                .isEqualTo(DockerImageName.parse("postgres:16"));
    }

    @Test
    void shouldUseImageFromEnvironmentVariable_WhenNoImageNameOrSystemPropertyProvided() {
        var systemPropertyLookup = (UnaryOperator<String>) key -> null;
        var envVarLookup = (UnaryOperator<String>) key -> "postgres:17";

        var image = PostgresLiquibaseTestcontainersExtension.resolveDefaultImage(
                null,
                systemPropertyLookup,
                envVarLookup);

        assertThat(image).isEqualTo(DockerImageName.parse("postgres:17"));
    }

    @Test
    void shouldPreferSystemProperty_OverEnvironmentVariable() {
        var image = PostgresLiquibaseTestcontainersExtension.resolveDefaultImage(
                null,
                key -> "postgres:16",
                key -> "postgres:17");

        assertThat(image).isEqualTo(DockerImageName.parse("postgres:16"));
    }

    @Test
    void shouldUseDefaultImage_WhenNoImageNameOrSystemPropertyOrEnvVar() {
        var extension = PostgresLiquibaseTestcontainersExtension.builder()
                .build();

        assertThat(extension.getImageName())
                .isEqualTo(DockerImageName.parse("postgres:18-alpine"));
    }

    @Test
    void shouldUseProvidedChangeLogPath() {
        var extension = PostgresLiquibaseTestcontainersExtension.builder()
                .changelogPath("some/path/custom-migrations.xml")
                .build();

        assertThat(extension.getChangelogPath()).isEqualTo("some/path/custom-migrations.xml");
    }

    @Test
    void shouldUseDefaultChangelogPath_WhenNotProvided() {
        var extension = PostgresLiquibaseTestcontainersExtension.builder().build();

        assertThat(extension.getChangelogPath()).isEqualTo("migrations.xml");
    }
}
