package org.kiwiproject.test.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.kiwiproject.time.KiwiInstants.minusDays;
import static org.kiwiproject.time.KiwiInstants.plusDays;

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder;
import org.assertj.core.data.TemporalUnitOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.time.Instant;

@DisplayName("CertOptions")
class CertOptionsTest {

    private static final TemporalUnitOffset WITHIN_100_MILLIS = within(Duration.ofMillis(100));

    @Test
    void shouldHaveDefaults() {
        var options = CertOptions.defaultOptions();
        var now = Instant.now();

        assertAll(
                () -> assertThat(options.notBefore()).isCloseTo(minusDays(now, 1), WITHIN_100_MILLIS),
                () -> assertThat(options.notAfter()).isCloseTo(plusDays(now, 1), WITHIN_100_MILLIS),
                () -> assertThat(options.caSubject()).isEqualTo("CN=TestCA, OU=TestUnit, O=TestOrg"),
                () -> assertThat(options.leafSubject()).isEqualTo("CN=Test, OU=TestUnit, O=TestOrg"),
                () -> assertThat(options.sanDnsName()).isEqualTo("example.org"),
                () -> assertThat(options.keyStoreAlgorithm()).isEqualTo("JKS"),
                () -> assertThat(options.keyStorePassword()).startsWith("key_pass_"),
                () -> assertThat(options.keyStoreFileName()).isEqualTo("keystore"),
                () -> assertThat(options.trustStoreAlgorithm()).isEqualTo("JKS"),
                () -> assertThat(options.trustStorePassword()).startsWith("trust_pass_"),
                () -> assertThat(options.trustStoreFileName()).isEqualTo("truststore"),
                () -> assertThat(options.certDir()).isNull()
        );
    }

    @Test
    void shouldValidateCertDir(@TempDir Path certDir) {
        assertThatCode(() -> CertOptions.builder().certDir(certDir).build())
                .doesNotThrowAnyException();
    }

    @Test
    void shouldThrow_WhenCertDir_DoesNotExist() {
        var dir = Path.of("/this/does/not/exist");
        assertThatIllegalStateException()
                .isThrownBy(() -> CertOptions.builder().certDir(dir).build())
                .withMessage("%s is not a directory or is not writable", dir);
    }

    @Test
    void shouldThrow_WhenCertDir_IsNotADirectory(@TempDir Path certDir) throws IOException {
        var file = certDir.resolve("a-file");
        Files.writeString(file, "some text");
        assertThatIllegalStateException()
                .isThrownBy(() -> CertOptions.builder().certDir(file).build())
                .withMessage("%s is not a directory or is not writable", file);
    }

    @Test
    void shouldThrow_WhenCertDir_IsNotWritable() throws IOException {
        try (var fileSystem = MemoryFileSystemBuilder.newLinux().build()) {
            var certDir = fileSystem.getPath("/certs");
            Files.createDirectories(certDir);
            Files.setPosixFilePermissions(certDir, PosixFilePermissions.fromString("r-x------"));
            assertThatIllegalStateException()
                    .isThrownBy(() -> CertOptions.builder().certDir(certDir).build())
                    .withMessage("%s is not a directory or is not writable", certDir);
        }
    }

    @Nested
    class CertDates {

        private Instant now;

        @BeforeEach
        void setUp() {
            now = Instant.now();
        }

        @Test
        void shouldBeUsedWhenBothNonNull() {
            var options = CertOptions.builder()
                    .notBefore(minusDays(now, 30))
                    .notAfter(plusDays(now, 90))
                    .build();

            assertAll(
                    () -> assertThat(options.notBefore()).isEqualTo(minusDays(now, 30)),
                    () -> assertThat(options.notAfter()).isEqualTo(plusDays(now, 90))
            );
        }

        @Test
        void shouldNotAllow_NotBefore_ToBeEqualOrAfter_NotAfter() {
            var expectedMessage = "notBefore date must come before notAfter date";
            assertAll(
                    () -> assertThatIllegalStateException()
                            .isThrownBy(() -> CertOptions.builder()
                                    .notBefore(now)
                                    .notAfter(now)
                                    .build())
                            .withMessage(expectedMessage),
                    () -> assertThatIllegalStateException()
                            .isThrownBy(() -> CertOptions.builder()
                                    .notBefore(now.plusSeconds(1))
                                    .notAfter(now)
                                    .build())
                            .withMessage(expectedMessage)
            );
        }

        @Test
        void shouldSetBoth_WhenTheyAreNull() {
            var options = CertOptions.builder().build();
            assertAll(
                    () -> assertThat(options.notBefore()).isCloseTo(minusDays(now, 1), WITHIN_100_MILLIS),
                    () -> assertThat(options.notAfter()).isCloseTo(plusDays(now, 1), WITHIN_100_MILLIS)
            );
        }

        @Test
        void shouldSetNotBefore_WhenOnly_NotAfterExists() {
            var notAfter = plusDays(now, 60);
            var options = CertOptions.builder()
                    .notAfter(notAfter)
                    .build();
            assertAll(
                    () -> assertThat(options.notBefore()).isEqualTo(minusDays(notAfter, 180)),
                    () -> assertThat(options.notAfter()).isSameAs(notAfter)
            );
        }

        @Test
        void shouldSetNotAfter_WhenOnly_NotBeforeExists() {
            var notBefore = minusDays(now, 5);
            var options = CertOptions.builder()
                    .notBefore(notBefore)
                    .build();
            assertAll(
                    () -> assertThat(options.notBefore()).isSameAs(notBefore),
                    () -> assertThat(options.notAfter()).isEqualTo(plusDays(notBefore, 180))
            );
        }
    }

    @Test
    void shouldGetKeyStorePasswordAsArray() {
        var password = "change_me";
        var options = CertOptions.builder()
                .keyStorePassword(password)
                .build();

        assertThat(options.keyStorePasswordAsArray()).isEqualTo(password.toCharArray());
    }

    @Test
    void shouldGetTrustStorePasswordAsArray() {
        var password = "change_me";
        var options = CertOptions.builder()
                .trustStorePassword(password)
                .build();

        assertThat(options.trustStorePasswordAsArray()).isEqualTo(password.toCharArray());
    }
}
