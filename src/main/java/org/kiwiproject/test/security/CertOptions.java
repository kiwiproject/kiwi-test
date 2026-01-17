package org.kiwiproject.test.security;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.firstNonBlank;
import static org.kiwiproject.time.KiwiInstants.minusDays;
import static org.kiwiproject.time.KiwiInstants.plusDays;

import com.google.common.annotations.Beta;
import lombok.Builder;
import org.jspecify.annotations.Nullable;
import org.kiwiproject.security.KeyStoreType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

/**
 * Options to use when creating the self-signed CA certificate and a certificate signed by that CA.
 * <p>
 * Validation at creation ensures the directory (when not null) is currently writable,
 * but filesystem state may change between option creation and certificate generation.
 *
 * @param notBefore           neither CA certificate nor issued certificate will be valid before this time
 * @param notAfter            neither CA certificate nor issued certificate will be valid after this time
 * @param caSubject           the subject of the CA certificate
 * @param leafSubject         the subject of the issued certificate
 * @param sanDnsName          the DNS name for the issued certificate's Subject Alternative Name (SAN) extension
 * @param keyStoreAlgorithm   the algorithm for the key store; default is "JKS"
 * @param keyStorePassword    the password for the key store; the default is "key_pass_[current-time-millis]"
 * @param keyStoreFileName    the file name for the key store; applies when certDir is not blank; default is "keystore"
 * @param trustStoreAlgorithm the algorithm for the trust store; default is "JKS"
 * @param trustStorePassword  the password for the trust store; the default is "trust_pass_[current-time-millis]"
 * @param trustStoreFileName  the file name for the trust store; applies when certDir is not blank; the default
 *                            is "truststore"
 * @param certDir             the directory where the key and trust store will be written; if null, the key and trust
 *                            stores will be in-memory and not written to disk
 */
@Beta
@Builder(toBuilder = true)
public record CertOptions(
        Instant notBefore,
        Instant notAfter,
        String caSubject,
        String leafSubject,
        String sanDnsName,
        String keyStoreAlgorithm,
        String keyStorePassword,
        String keyStoreFileName,
        String trustStoreAlgorithm,
        String trustStorePassword,
        String trustStoreFileName,
        @Nullable Path certDir
) {

    public CertOptions {
        validateCertDir(certDir);

        var certDates = certDates(notBefore, notAfter);
        notBefore = certDates.notBefore();
        notAfter = certDates.notAfter();

        caSubject = firstNonBlank(caSubject, "CN=TestCA, OU=TestUnit, O=TestOrg");
        leafSubject = firstNonBlank(leafSubject, "CN=Test, OU=TestUnit, O=TestOrg");
        sanDnsName = firstNonBlank(sanDnsName, "example.org");
        keyStoreAlgorithm = firstNonBlank(keyStoreAlgorithm, KeyStoreType.JKS.getValue());
        keyStorePassword = firstNonBlank(keyStorePassword, "key_pass_" + System.currentTimeMillis());
        keyStoreFileName = firstNonBlank(keyStoreFileName, "keystore");
        trustStoreAlgorithm = firstNonBlank(trustStoreAlgorithm, KeyStoreType.JKS.getValue());
        trustStorePassword = firstNonBlank(trustStorePassword, "trust_pass_" + System.currentTimeMillis());
        trustStoreFileName = firstNonBlank(trustStoreFileName, "truststore");
    }

    private static void validateCertDir(@Nullable Path certDir) {
        if (nonNull(certDir)) {
            checkState(Files.isDirectory(certDir) && Files.isWritable(certDir),
                    "%s is not a directory or is not writable", certDir);
        }
    }

    /**
     * Derives certificate validity dates based on the provided inputs.
     * <ul>
     *   <li>
     *       If both dates are null, defaults to plus and minues 1 day around now.
     *   </li>
     *   <li>
     *       If only one bound is provided, the other is derived using a
     *       180-day window, intended for long-lived test certificates.
     *   </li>
     * </ul>
     */
    private static CertDates certDates(@Nullable Instant notBefore,
                                       @Nullable Instant notAfter) {

        if (nonNull(notBefore) && nonNull(notAfter)) {
            checkState(notAfter.isAfter(notBefore), "notBefore date must come before notAfter date");
            return new CertDates(notBefore, notAfter);
        }

        if (isNull(notBefore) && isNull(notAfter)) {
            var now = Instant.now();
            var notBefore1 = minusDays(now, 1);
            var notAfter1 = plusDays(now, 1);
            return new CertDates(notBefore1, notAfter1);
        }

        if (nonNull(notBefore)) {
            var notAfter1 = plusDays(notBefore, 180);
            return new CertDates(notBefore, notAfter1);
        }

        var notBefore1 = minusDays(notAfter, 180);
        return new CertDates(notBefore1, notAfter);
    }

    private record CertDates(Instant notBefore, Instant notAfter) {
    }

    /**
     * Create a new {@link CertOptions} with default values.
     *
     * @return a new instance
     */
    public static CertOptions defaultOptions() {
        return CertOptions.builder().build();
    }

    /**
     * Convenience method to get the key store password as a char array.
     *
     * @return the key store password as a char array
     */
    public char[] keyStorePasswordAsArray() {
        return keyStorePassword.toCharArray();
    }

    /**
     * Convenience method to get the trust store password as a char array.
     *
     * @return the trust store password as a char array
     */
    public char[] trustStorePasswordAsArray() {
        return trustStorePassword.toCharArray();
    }
}
