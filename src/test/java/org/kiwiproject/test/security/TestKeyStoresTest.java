package org.kiwiproject.test.security;

import static io.netty.pkitesting.CertificateBuilder.ExtendedKeyUsage;
import static io.netty.pkitesting.CertificateBuilder.KeyUsage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.jupiter.api.Assertions.assertAll;

import io.netty.pkitesting.CertificateBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

@DisplayName("TestKeyStores")
class TestKeyStoresTest {

    private Path dir;
    private TestKeyStores keyStores;
    private String password;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        var template = new CertificateBuilder();
        var issuerBundle = template.copy()
                .subject("CN=TestCA, OU=Dept42, O=YourOrg")
                .setKeyUsage(true, KeyUsage.digitalSignature, KeyUsage.keyCertSign)
                .setIsCertificateAuthority(true)
                .buildSelfSigned();
        var leafBundle = template.copy()
                .subject("CN=Test, OU=DeptA42, O=YourOrg")
                .setKeyUsage(true, KeyUsage.digitalSignature)
                .addExtendedKeyUsage(ExtendedKeyUsage.PKIX_KP_SERVER_AUTH)
                .addSanDnsName("san-1.leaf.dept42.yourorg.com")
                .buildIssuedBy(issuerBundle);

        dir = tempDir;
        password = "change me";
        keyStores = TestKeyStores.builder()
                .keyStore(leafBundle.toKeyStore(password.toCharArray()))
                .keyStorePath(dir)
                .keyStorePassword(password)
                .trustStore(issuerBundle.toKeyStore(password.toCharArray()))
                .trustStorePath(dir)
                .trustStorePassword(password)
                .issuerBundle(issuerBundle)
                .leafBundle(leafBundle)
                .build();
    }

    @Test
    void shouldGetRequiredKeyStorePath() {
        assertAll(
                () -> assertThat(keyStores.requiredKeyStorePath()).isEqualTo(dir),
                () -> assertThat(keyStores.requiredKeyStorePathAsString()).isEqualTo(dir.toString())
        );
    }

    @Test
    void shouldThrowIllegalStateException_WhenKeyStorePath_IsNull() {
        keyStores = keyStores.toBuilder()
                .keyStorePath(null)
                .build();
        assertAll(
                () -> assertThatIllegalStateException()
                        .isThrownBy(() -> keyStores.requiredKeyStorePath())
                        .withMessage("Key store path is null (key store is in-memory)"),
                () -> assertThatIllegalStateException()
                        .isThrownBy(() -> keyStores.requiredKeyStorePathAsString())
                        .withMessage("Key store path is null (key store is in-memory)")
        );
    }

    @Test
    void shouldGetRequiredTrustStorePath() {
        assertAll(
                () -> assertThat(keyStores.requiredTrustStorePath()).isEqualTo(dir),
                () -> assertThat(keyStores.requiredTrustStorePathAsString()).isEqualTo(dir.toString())
        );
    }

    @Test
    void shouldThrowIllegalStateException_WhenTrustStorePath_IsNull() {
        keyStores = keyStores.toBuilder()
                .trustStorePath(null)
                .build();
        assertAll(
                () -> assertThatIllegalStateException()
                        .isThrownBy(() -> keyStores.requiredTrustStorePath())
                        .withMessage("Trust store path is null (trust store is in-memory)"),
                () -> assertThatIllegalStateException()
                        .isThrownBy(() -> keyStores.requiredTrustStorePathAsString())
                        .withMessage("Trust store path is null (trust store is in-memory)")
        );
    }

    @Test
    void shouldGetKeyStorePasswordAsArray() {
        assertThat(keyStores.keyStorePasswordAsArray()).isEqualTo(password.toCharArray());
    }

    @Test
    void shouldGetTrustStorePasswordAsArray() {
        assertThat(keyStores.trustStorePasswordAsArray()).isEqualTo(password.toCharArray());
    }
}
