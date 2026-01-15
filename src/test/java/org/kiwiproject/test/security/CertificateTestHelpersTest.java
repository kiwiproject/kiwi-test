package org.kiwiproject.test.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.kiwiproject.time.KiwiInstants.minusDays;
import static org.kiwiproject.time.KiwiInstants.plusDays;

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder;
import io.netty.pkitesting.CertificateBuilder;
import io.netty.pkitesting.CertificateBuilder.Algorithm;
import org.apache.commons.lang3.exception.UncheckedException;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.kiwiproject.collect.KiwiLists;
import org.kiwiproject.security.KeyStoreType;
import org.kiwiproject.security.UncheckedGeneralSecurityException;
import org.kiwiproject.util.function.KiwiConsumers;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

@DisplayName("CertificateTestHelpers")
class CertificateTestHelpersTest {

    private static final String JKS = KeyStoreType.JKS.getValue();
    private static final String PKCS12 = KeyStoreType.PKCS12.getValue();

    @Test
    void shouldThrowIllegalStateException_WhenCertDirDoesNotExist() {
        var dir = Path.of("/foo/bar/baz");
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> CertificateTestHelpers.createJKSKeyAndTrustStores(dir))
                .withMessageContaining("/foo/bar/baz is not a directory or is not writable");
    }

    @Test
    void shouldThrowUncheckedIOException_WhenCertDirIsNotWritable() throws IOException {
        try (var fileSystem = MemoryFileSystemBuilder.newLinux().build()) {
            var certDir = fileSystem.getPath("/certs");
            Files.createDirectories(certDir);

            // Build options while the directory is writable
            var options = CertOptions.builder()
                    .certDir(certDir)
                    .build();

            // Now make it unwritable
            var permissions = PosixFilePermissions.fromString("r-xr--r--");
            Files.setPosixFilePermissions(certDir, permissions);

            assertThatExceptionOfType(UncheckedIOException.class)
                    .isThrownBy(() -> CertificateTestHelpers.createKeyAndTrustStores(options))
                    .withMessage("Error saving key or trust store")
                    .havingCause()
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Test
    void shouldThrowUncheckedIOException_WhenFileAlreadyExists() throws IOException {
        try (var fileSystem = MemoryFileSystemBuilder.newLinux().build()) {
            var certDir = fileSystem.getPath("/certs");
            Files.createDirectories(certDir);

            var options = CertOptions.builder()
                    .certDir(certDir)
                    .build();

            // Write sometime to the keystore file so it exists
            var path = certDir.resolve(options.keyStoreFileName());
            Files.writeString(path, "some random text");

            assertThatExceptionOfType(UncheckedIOException.class)
                    .isThrownBy(() -> CertificateTestHelpers.createKeyAndTrustStores(options))
                    .withMessage("Error saving key or trust store")
                    .havingCause()
                    .isInstanceOf(FileAlreadyExistsException.class);
        }
    }

    @Test
    void shouldThrowUncheckedGeneralSecurityException_WhenSomeSecurityProblemHappens() {
        var options = CertOptions.builder()
                .keyStoreAlgorithm("FOO")
                .build();

        assertThatExceptionOfType(UncheckedGeneralSecurityException.class)
                .isThrownBy(() -> CertificateTestHelpers.createKeyAndTrustStores(options))
                .withMessage("Security-related error creating key or trust store")
                .havingCause()
                .isInstanceOf(KeyStoreException.class)
                .havingCause()
                .isInstanceOf(NoSuchAlgorithmException.class);
    }

    @Test
    void shouldThrowUncheckedException_WhenSomeRandomBadThingHappens() {
        var options = CertOptions.defaultOptions();
        var caCustomizer = KiwiConsumers.<CertificateBuilder>noOp();
        Consumer<CertificateBuilder> leafCustomizer = leafCertBuilder -> {
            throw new RuntimeException("oops");
        };

        assertThatExceptionOfType(UncheckedException.class)
                .isThrownBy(() -> CertificateTestHelpers.createKeyAndTrustStores(options, caCustomizer, leafCustomizer))
                .havingCause()
                .isExactlyInstanceOf(RuntimeException.class)
                .withMessage("oops");
    }

    @Test
    void shouldCreateInMemoryJKSKeyAndTrustStores() {
        var keyStores = CertificateTestHelpers.createInMemoryJKSKeyAndTrustStores();

        assertAll(
                () -> assertThat(keyStores.keyStore()).isNotNull(),
                () -> assertThat(keyStores.keyStorePassword()).isNotBlank(),
                () -> assertThat(keyStores.keyStorePath()).isNull(),
                () -> assertThat(keyStores.trustStore()).isNotNull(),
                () -> assertThat(keyStores.trustStorePassword()).isNotBlank(),
                () -> assertThat(keyStores.trustStorePath()).isNull(),
                () -> assertThat(keyStores.issuerBundle()).isNotNull(),
                () -> assertThat(keyStores.leafBundle()).isNotNull()
        );

        assertAll(
                () -> assertThat(keyStores.keyStore().getType()).isEqualTo(JKS),
                () -> assertThat(keyStores.trustStore().getType()).isEqualTo(JKS)
        );
    }

    @Test
    void shouldCreateOnDiskJKSKeyAndTrustStores(@TempDir Path certDir) {
        var keyStores = CertificateTestHelpers.createJKSKeyAndTrustStores(certDir);

        assertAll(
                () -> assertThat(keyStores.keyStore()).isNotNull(),
                () -> assertThat(keyStores.keyStorePassword()).isNotBlank(),
                () -> assertThat(keyStores.keyStorePath()).startsWith(certDir),
                () -> assertThat(keyStores.trustStore()).isNotNull(),
                () -> assertThat(keyStores.trustStorePassword()).isNotBlank(),
                () -> assertThat(keyStores.trustStorePath()).startsWith(certDir),
                () -> assertThat(keyStores.issuerBundle()).isNotNull(),
                () -> assertThat(keyStores.leafBundle()).isNotNull()
        );

        assertAll(
                () -> assertThat(keyStores.keyStore().getType()).isEqualTo(JKS),
                () -> assertThat(keyStores.trustStore().getType()).isEqualTo(JKS)
        );
    }

    @Test
    void shouldCreateInMemoryKeyAndTrustStores() {
        var keyStores = CertificateTestHelpers.createInMemoryKeyAndTrustStores(PKCS12, PKCS12);

        assertAll(
                () -> assertThat(keyStores.keyStore()).isNotNull(),
                () -> assertThat(keyStores.keyStorePassword()).isNotBlank(),
                () -> assertThat(keyStores.keyStorePath()).isNull(),
                () -> assertThat(keyStores.trustStore()).isNotNull(),
                () -> assertThat(keyStores.trustStorePassword()).isNotBlank(),
                () -> assertThat(keyStores.trustStorePath()).isNull(),
                () -> assertThat(keyStores.issuerBundle()).isNotNull(),
                () -> assertThat(keyStores.leafBundle()).isNotNull()
        );

        assertAll(
                () -> assertThat(keyStores.keyStore().getType()).isEqualTo(PKCS12),
                () -> assertThat(keyStores.trustStore().getType()).isEqualTo(PKCS12)
        );
    }

    @Test
    void shouldCreateKeyAndTrustStoresWithCertOptions(@TempDir Path certDir) {
        var now = Instant.now();

        // The cert not-before/not-after dates only go to seconds, so must truncate to seconds
        var notBefore = minusDays(now, 30).truncatedTo(ChronoUnit.SECONDS);
        var notAfter = plusDays(now, 90).truncatedTo(ChronoUnit.SECONDS);

        var options = CertOptions.builder()
                .notBefore(notBefore)
                .notAfter(notAfter)
                .caSubject("CN=MyCA, OU=MyUnit, O=MyOrg")
                .leafSubject("CN=MyTest, OU=MyUnit, O=MyOrg")
                .sanDnsName("test.org")
                .keyStoreAlgorithm(PKCS12)
                .keyStorePassword("my_key_pwd")
                .keyStoreFileName("my_ks")
                .trustStoreAlgorithm(PKCS12)
                .trustStorePassword("my_ts_pwd")
                .trustStoreFileName("my_ts")
                .certDir(certDir)
                .build();

        var keyStores = CertificateTestHelpers.createKeyAndTrustStores(options);

        assertAll(
                () -> assertThat(keyStores.keyStore()).isNotNull(),
                () -> assertThat(keyStores.keyStorePassword()).isEqualTo(options.keyStorePassword()),
                () -> assertThat(keyStores.keyStorePath())
                        .isEqualTo(Path.of(certDir.toString(), options.keyStoreFileName())),
                () -> assertThat(keyStores.trustStore()).isNotNull(),
                () -> assertThat(keyStores.trustStorePassword()).isEqualTo(options.trustStorePassword()),
                () -> assertThat(keyStores.trustStorePath())
                        .isEqualTo(Path.of(certDir.toString(), options.trustStoreFileName())),
                () -> assertThat(keyStores.issuerBundle()).isNotNull(),
                () -> assertThat(keyStores.leafBundle()).isNotNull()
        );

        var rootCert = keyStores.issuerBundle().getRootCertificate();
        var leafCert = keyStores.leafBundle().getCertificate();

        assertAll(
                () -> assertThat(keyStores.keyStore().getType()).isEqualTo(PKCS12),
                () -> assertThat(keyStores.trustStore().getType()).isEqualTo(PKCS12),

                () -> assertThat(rootCert.getNotBefore()).isEqualTo(Date.from(options.notBefore())),
                () -> assertThat(rootCert.getNotAfter()).isEqualTo(Date.from(options.notAfter())),
                () -> assertThat(rootCert.getKeyUsage()[0]).isTrue(),  // digitalSignature
                () -> assertThat(rootCert.getKeyUsage()[5]).isTrue(),  // keyCertSign

                () -> assertThat(leafCert.getNotBefore()).isEqualTo(Date.from(options.notBefore())),
                () -> assertThat(leafCert.getNotAfter()).isEqualTo(Date.from(options.notAfter())),
                () -> assertThat(extractSubjectAltNames(leafCert))
                        .containsExactlyInAnyOrder(options.sanDnsName()),
                () -> assertThat(leafCert.getKeyUsage()[0]).isTrue(),  // digitalSignature
                () -> assertThat(leafCert.getExtendedKeyUsage())
                        .contains(KeyPurposeId.id_kp_serverAuth.getId())
        );
    }

    @Test
    void shouldCreateKeyAndTrustStoresWithCertOptionsAndCustomizers(@TempDir Path certDir) {
        var now = Instant.now();

        var options = CertOptions.builder()
                .notBefore(minusDays(now, 30))
                .notAfter(plusDays(now, 90))
                .caSubject("CN=MyCA, OU=MyUnit, O=MyOrg")
                .leafSubject("CN=MyTest, OU=MyUnit, O=MyOrg")
                .sanDnsName("test.org")
                .keyStoreAlgorithm(PKCS12)
                .keyStorePassword("my_key_pwd")
                .keyStoreFileName("my_ks")
                .trustStoreAlgorithm(PKCS12)
                .trustStorePassword("my_ts_pwd")
                .trustStoreFileName("my_ts")
                .certDir(certDir)
                .build();

        var keyStores = CertificateTestHelpers.createKeyAndTrustStores(options,
                caCertBuilder -> caCertBuilder.algorithm(Algorithm.ed25519)
                        .addSanDnsName("test.org")
                        .addSanIpAddress("10.116.78.42")
                        .addExtendedKeyUsage(CertificateBuilder.ExtendedKeyUsage.PKIX_KP_OCSP_SIGNING),
                leafCertBuilder -> leafCertBuilder.algorithm(Algorithm.ed25519)
                        .addSanIpAddress("10.116.78.180"));

        assertAll(
                () -> assertThat(keyStores.keyStore()).isNotNull(),
                () -> assertThat(keyStores.keyStorePassword()).isEqualTo(options.keyStorePassword()),
                () -> assertThat(keyStores.keyStorePath())
                        .isEqualTo(Path.of(certDir.toString(), options.keyStoreFileName())),
                () -> assertThat(keyStores.trustStore()).isNotNull(),
                () -> assertThat(keyStores.trustStorePassword()).isEqualTo(options.trustStorePassword()),
                () -> assertThat(keyStores.trustStorePath())
                        .isEqualTo(Path.of(certDir.toString(), options.trustStoreFileName())),
                () -> assertThat(keyStores.issuerBundle()).isNotNull(),
                () -> assertThat(keyStores.leafBundle()).isNotNull()
        );

        assertAll(
                () -> assertThat(keyStores.keyStore().getType()).isEqualTo(PKCS12),
                () -> assertThat(keyStores.trustStore().getType()).isEqualTo(PKCS12)
        );

        // Check the customizations
        var expectedAlgName = Algorithm.ed25519.name();
        var rootCert = keyStores.issuerBundle().getRootCertificate();
        var leafCert = keyStores.leafBundle().getCertificate();

        assertAll(
                () -> assertThat(rootCert.getSigAlgName()).isEqualToIgnoringCase(expectedAlgName),
                () -> assertThat(extractSubjectAltNames(rootCert))
                        .containsExactlyInAnyOrder("test.org", "10.116.78.42"),
                () -> assertThat(rootCert.getExtendedKeyUsage())
                        .contains(KeyPurposeId.id_kp_OCSPSigning.getId()),

                () -> assertThat(leafCert.getSigAlgName()).isEqualToIgnoringCase(expectedAlgName),
                () -> assertThat(extractSubjectAltNames(leafCert)).contains("10.116.78.180")
        );
    }

    @Test
    void shouldCreateTrustStore_ContainingIssuerCert() {
        var options = CertOptions.defaultOptions();

        var keyStores = CertificateTestHelpers.createKeyAndTrustStores(options);

        var trustStore = keyStores.trustStore();
        var aliases = getAliases(trustStore);
        var certificates = aliases
                .stream()
                .map(alias -> getCertificate(trustStore, alias))
                .toList();

        var issuerBundle = keyStores.issuerBundle();

        // All entries resolve to the issuer cert because the trust store
        // contains only CA material (key entry + trusted cert entry)
        assertThat(certificates)
                .isNotEmpty()
                .allMatch(cert -> cert.equals(issuerBundle.getRootCertificate()));

        assertThat(aliases)
                .anyMatch(alias -> isCertificateEntry(trustStore, alias))
                .anyMatch(alias -> isKeyEntry(trustStore, alias));

        var issuedCert = keyStores.leafBundle().getCertificate();
        var issuerCert = issuerBundle.getRootCertificate();
        verify(issuerCert, issuedCert);
    }

    private static List<String> getAliases(KeyStore keyStore) {
        try {
            return Collections.list(keyStore.aliases());
        } catch (KeyStoreException e) {
            throw new UncheckedGeneralSecurityException(e);
        }
    }

    private static Certificate getCertificate(KeyStore keyStore, String alias) {
        try {
            return keyStore.getCertificate(alias);
        } catch (KeyStoreException e) {
            throw new UncheckedGeneralSecurityException(e);
        }
    }

    private static boolean isCertificateEntry(KeyStore keyStore, String alias) {
        try {
            return keyStore.isCertificateEntry(alias);
        } catch (KeyStoreException e) {
            throw new UncheckedGeneralSecurityException(e);
        }
    }

    private static boolean isKeyEntry(KeyStore keyStore, String alias) {
        try {
            return keyStore.isKeyEntry(alias);
        } catch (KeyStoreException e) {
            throw new UncheckedGeneralSecurityException(e);
        }
    }

    private static void verify(Certificate issuerCert, Certificate issuedCert) {
        try {
            var issuerPublicKey = issuerCert.getPublicKey();
            issuedCert.verify(issuerPublicKey);
        } catch (GeneralSecurityException e) {
            throw new UncheckedGeneralSecurityException(e);
        }
    }

    @Test
    void shouldCreateKeyAndTrustStoresWithCACustomizer() {
        var options = CertOptions.defaultOptions();

        var keyStores = CertificateTestHelpers.createKeyAndTrustStoresWithCACustomizer(options,
                caCertBuilder -> caCertBuilder.algorithm(Algorithm.ed25519)
                        .addSanDnsName("test.org")
                        .addSanIpAddress("10.116.78.42")
                        .addExtendedKeyUsage(CertificateBuilder.ExtendedKeyUsage.PKIX_KP_OCSP_SIGNING));

        // Check the customizations
        var expectedAlgName = Algorithm.ed25519.name();
        var rootCert = keyStores.issuerBundle().getRootCertificate();

        assertAll(
                () -> assertThat(rootCert.getSigAlgName()).isEqualToIgnoringCase(expectedAlgName),
                () -> assertThat(extractSubjectAltNames(rootCert))
                        .containsExactlyInAnyOrder("test.org", "10.116.78.42"),
                () -> assertThat(rootCert.getExtendedKeyUsage())
                        .contains(KeyPurposeId.id_kp_OCSPSigning.getId())
        );
    }

    @Test
    void shouldCreateKeyAndTrustStoresWithLeafCustomizer() {
        var options = CertOptions.defaultOptions();

        var keyStores = CertificateTestHelpers.createKeyAndTrustStoresWithLeafCustomizer(options,
                leafCertBuilder -> leafCertBuilder.algorithm(Algorithm.ed25519)
                        .addSanIpAddress("10.116.78.180")
                        .addSanDnsName("sub.example.org")
                        .addSanDirectoryName("CN=ldap.example.org"));

        // Check the customizations
        var rootCert = keyStores.issuerBundle().getRootCertificate();
        var leafCert = keyStores.leafBundle().getCertificate();

        assertAll(
                () -> assertThat(leafCert.getSigAlgName())
                        .describedAs("leaf cert should have same algorithm as root cert")
                        .isEqualTo(rootCert.getSigAlgName()),
                () -> assertThat(extractSubjectAltNames(leafCert)).containsExactlyInAnyOrder(
                        "10.116.78.180",
                        "sub.example.org",
                        options.sanDnsName(),
                        "CN=ldap.example.org")
        );
    }

    private static List<String> extractSubjectAltNames(X509Certificate cert) {
        try {
            return cert.getSubjectAlternativeNames()
                    .stream()
                    .map(KiwiLists::second)
                    .map(Object::toString)
                    .toList();
        } catch (CertificateParsingException e) {
            throw new UncheckedGeneralSecurityException(e);
        }
    }
}
