package org.kiwiproject.test.security;

import static java.util.Objects.nonNull;
import static org.kiwiproject.base.KiwiPreconditions.checkArgumentNotNull;

import com.google.common.annotations.Beta;
import io.netty.pkitesting.CertificateBuilder;
import io.netty.pkitesting.X509Bundle;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.exception.UncheckedException;
import org.jspecify.annotations.Nullable;
import org.kiwiproject.security.KeyStoreType;
import org.kiwiproject.security.UncheckedGeneralSecurityException;
import org.kiwiproject.util.function.KiwiConsumers;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.function.Consumer;

/**
 * Utilities to create certs for unit tests.
 *
 * @implNote Uses {@code netty-pkitesting} under the hood, so it must be available on the classpath.
 */
@Beta
@UtilityClass
public class CertificateTestHelpers {

    private static final String JKS = KeyStoreType.JKS.getValue();

    /**
     * Create in-memory JKS key and trust stores using default values.
     *
     * @return a {@link TestKeyStores} containing information to access the certs
     */
    public static TestKeyStores createInMemoryJKSKeyAndTrustStores() {
        return createKeyAndTrustStores(null, JKS, JKS);
    }

    /**
     * Create in-memory JKS key and trust stores using the specified algorithms.
     *
     * @param keyStoreAlgorithm   the algorithm to use for the key store
     * @param trustStoreAlgorithm the algorithm to use for the trust store
     * @return a {@link TestKeyStores} containing information to access the certs
     */
    public static TestKeyStores createInMemoryKeyAndTrustStores(String keyStoreAlgorithm,
                                                                String trustStoreAlgorithm) {
        return createKeyAndTrustStores(null, keyStoreAlgorithm, trustStoreAlgorithm);
    }

    /**
     * Create JKS key and trust stores using the specified algorithms and write them
     * to the specified directory or in-memory if null.
     *
     * @param certDir the directory where the certs will be stored, or null for in-memory stores
     * @return a {@link TestKeyStores} containing information to access the certs
     */
    public static TestKeyStores createJKSKeyAndTrustStores(@Nullable Path certDir) {
        return createKeyAndTrustStores(certDir, JKS, JKS);
    }

    /**
     * Create key and trust stores using the specified algorithms and write them
     * to the specified directory or in-memory if null.
     *
     * @param certDir             the directory where the certs will be stored, or null for in-memory stores
     * @param keyStoreAlgorithm   the algorithm to use for the key store
     * @param trustStoreAlgorithm the algorithm to use for the trust store
     * @return a {@link TestKeyStores} containing information to access the certs
     */
    public static TestKeyStores createKeyAndTrustStores(@Nullable Path certDir,
                                                        String keyStoreAlgorithm,
                                                        String trustStoreAlgorithm) {

        var options = CertOptions.builder()
                .certDir(certDir)
                .keyStoreAlgorithm(keyStoreAlgorithm)
                .trustStoreAlgorithm(trustStoreAlgorithm)
                .build();

        return createKeyAndTrustStores(options);
    }

    /**
     * Create key and trust stores using the specified options.
     *
     * @param options the configuration options for creating the key and trust stores
     * @return a {@link TestKeyStores} containing information to access the certs
     */
    public static TestKeyStores createKeyAndTrustStores(CertOptions options) {
        return createKeyAndTrustStores(options, KiwiConsumers.noOp(), KiwiConsumers.noOp());
    }

    /**
     * Create key and trust stores using the specified options and CA certificate customizer.
     * The consumer argument allows further customization of the CA certificate.
     *
     * @param options      the configuration options for creating the key and trust stores
     * @param caCustomizer a customizer for the CA certificate builder
     * @return a {@link TestKeyStores} containing information to access the certs
     */
    public static TestKeyStores createKeyAndTrustStoresWithCACustomizer(CertOptions options,
                                                                        Consumer<CertificateBuilder> caCustomizer) {
        return createKeyAndTrustStores(options, caCustomizer, KiwiConsumers.noOp());
    }

    /**
     * Create key and trust stores using the specified options and leaf certificate customizer.
     * The consumer argument allows further customization of the leaf certificate.
     *
     * @param options        the configuration options for creating the key and trust stores
     * @param leafCustomizer a customizer for the leaf certificate builder
     * @return a {@link TestKeyStores} containing information to access the certs
     */
    public static TestKeyStores createKeyAndTrustStoresWithLeafCustomizer(CertOptions options,
                                                                          Consumer<CertificateBuilder> leafCustomizer) {
        return createKeyAndTrustStores(options, KiwiConsumers.noOp(), leafCustomizer);
    }

    /**
     * Create key and trust stores using the specified options and customizers.
     * The consumer arguments allow further customization of the CA and leaf certificates.
     *
     * @param options        the configuration options for creating the key and trust stores
     * @param caCustomizer   a customizer for the CA certificate builder
     * @param leafCustomizer a customizer for the leaf certificate builder
     * @return a {@link TestKeyStores} containing information to access the certs
     */
    public static TestKeyStores createKeyAndTrustStores(CertOptions options,
                                                        Consumer<CertificateBuilder> caCustomizer,
                                                        Consumer<CertificateBuilder> leafCustomizer) {

        checkArgumentNotNull(options, "options must not be null");
        checkArgumentNotNull(caCustomizer, "caCustomizer must not be null");
        checkArgumentNotNull(leafCustomizer, "leafCustomizer must not be null");

        try {
            var template = new CertificateBuilder()
                    .notBefore(options.notBefore())
                    .notAfter(options.notAfter());

            // Build CA cert
            var issuerCertificateBuilder = template.copy()
                    .subject(options.caSubject())
                    .setKeyUsage(true,
                            CertificateBuilder.KeyUsage.digitalSignature,
                            CertificateBuilder.KeyUsage.keyCertSign)
                    .setIsCertificateAuthority(true);

            // Allow customization/overrides of CA cert
            caCustomizer.accept(issuerCertificateBuilder);

            X509Bundle issuer = issuerCertificateBuilder.buildSelfSigned();

            // Build leaf cert, signed by CA
            var leafCertificateBuilder = template.copy()
                    .subject(options.leafSubject())
                    .setKeyUsage(true, CertificateBuilder.KeyUsage.digitalSignature)
                    .addExtendedKeyUsage(CertificateBuilder.ExtendedKeyUsage.PKIX_KP_SERVER_AUTH)
                    .addSanDnsName(options.sanDnsName());

            // Allow customization/overrides of leaf cert
            leafCustomizer.accept(leafCertificateBuilder);

            X509Bundle leaf = leafCertificateBuilder.buildIssuedBy(issuer);

            // Generate key and trust stores
            var keyPassword = options.keyStorePassword();
            var keyPasswordCharArray = keyPassword.toCharArray();
            var keyStore = leaf.toKeyStore(options.keyStoreAlgorithm(), keyPasswordCharArray);

            var trustPassword = options.trustStorePassword();
            var trustPasswordCharArray = trustPassword.toCharArray();
            var trustStore = issuer.toKeyStore(options.trustStoreAlgorithm(), trustPasswordCharArray);

            var keyStoresBuilder = TestKeyStores.builder()
                    .keyStore(keyStore)
                    .keyStorePassword(keyPassword)
                    .trustStore(trustStore)
                    .trustStorePassword(trustPassword)
                    .issuerBundle(issuer)
                    .leafBundle(leaf);

            var certDir = options.certDir();
            if (nonNull(certDir)) {
                var keyStorePath = certDir.resolve(options.keyStoreFileName());
                store(keyStore, keyStorePath, keyPasswordCharArray);

                var trustStorePath = certDir.resolve(options.trustStoreFileName());
                store(trustStore, trustStorePath, trustPasswordCharArray);

                keyStoresBuilder.keyStorePath(keyStorePath).trustStorePath(trustStorePath);
            }

            return keyStoresBuilder.build();

        } catch (IOException e) {
            throw new UncheckedIOException("Error saving key or trust store", e);
        } catch (GeneralSecurityException e) {
            throw new UncheckedGeneralSecurityException("Security-related error creating key or trust store", e);
        } catch (Exception e) {
            throw new UncheckedException(e);
        }
    }

    private static void store(KeyStore keyStore, Path target, char[] password)
            throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {

        // Use CREATE_NEW to ensure we never overwrite existing keystores
        try (var outputStream = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
            keyStore.store(outputStream, password);
        }
    }
}
