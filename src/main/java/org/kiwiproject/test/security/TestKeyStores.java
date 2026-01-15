package org.kiwiproject.test.security;

import static org.kiwiproject.base.KiwiPreconditions.checkArgumentNotBlank;
import static org.kiwiproject.base.KiwiPreconditions.checkArgumentNotNull;

import com.google.common.annotations.Beta;
import io.netty.pkitesting.X509Bundle;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Optional;

/**
 * Holds key and trust stores that can be used for testing.
 * They can be on-disk or in-memory.
 *
 * @param keyStore           the key store
 * @param keyStorePassword   the key store password
 * @param keyStorePath       the key store path, or null if the key store is in-memory
 * @param trustStore         the trust store
 * @param trustStorePassword the trust store password
 * @param trustStorePath     the trust store path, or null if the trust store is in-memory
 * @param issuerBundle       the {@link X509Bundle} that represents the CA that issued the certs
 * @param leafBundle         the {@link X509Bundle} that represents the issued certs (key and trust store)
 */
@Beta
@Builder(toBuilder = true)
public record TestKeyStores(
        KeyStore keyStore,
        String keyStorePassword,
        @Nullable Path keyStorePath,
        KeyStore trustStore,
        String trustStorePassword,
        @Nullable Path trustStorePath,
        X509Bundle issuerBundle,
        X509Bundle leafBundle) {

    public TestKeyStores {
        checkArgumentNotNull(keyStore, "keyStore must not be null");
        checkArgumentNotBlank(keyStorePassword, "keyStorePassword must not be blank");
        checkArgumentNotNull(trustStore, "trustStore must not be null");
        checkArgumentNotBlank(trustStorePassword, "trustStorePassword must not be blank");
        checkArgumentNotNull(issuerBundle, "issuerBundle must not be null");
        checkArgumentNotNull(leafBundle, "leafBundle must not be null");
    }
    
    /**
     * Uses this to get the key store path when it is on-disk.
     *
     * @return the key store path
     * @throws IllegalStateException if the key store path is null
     */
    public Path requiredKeyStorePath() {
        return Optional.ofNullable(keyStorePath)
                .orElseThrow(() -> new IllegalStateException("Key store path is null (key store is in-memory)"));
    }

    /**
     * Use this to get the key store path as a String when it is on disk.
     *
     * @return the key store path as a string
     * @throws IllegalStateException if the key store path is null
     */
    public String requiredKeyStorePathAsString() {
        return requiredKeyStorePath().toString();
    }

    /**
     * Uses this to get the trust store path when it is on-disk.
     *
     * @return the trust store path
     * @throws IllegalStateException if the trust store path is null
     */
    public Path requiredTrustStorePath() {
        return Optional.ofNullable(trustStorePath)
                .orElseThrow(() -> new IllegalStateException("Trust store path is null (trust store is in-memory)"));
    }

    /**
     * Use this to get the trust store path as a String when it is on disk.
     *
     * @return the trust store path as a string
     * @throws IllegalStateException if the trust store path is null
     */
    public String requiredTrustStorePathAsString() {
        return requiredTrustStorePath().toString();
    }

    /**
     * Convenience method to get the key store password as an array.
     *
     * @return the key store password
     */
    public char[] keyStorePasswordAsArray() {
        return keyStorePassword.toCharArray();
    }

    /**
     * Convenience method to get the trust store password as an array.
     *
     * @return the trust store password
     */
    public char[] trustStorePasswordAsArray() {
        return trustStorePassword.toCharArray();
    }
}
