package com.sequenceiq.redbeams.configuration;

import static java.util.Objects.requireNonNull;

import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Objects;

public record SslCertificateEntry(
        int version,
        String cloudKey,
        String cloudProviderIdentifier,
        String cloudPlatform,
        String certPem,
        X509Certificate x509Cert,
        String fingerprint,
        long expirationDate,
        boolean deprecated
) {

    //Only for tests
    public SslCertificateEntry(int version,
            String cloudKey,
            String cloudProviderIdentifier,
            String cloudPlatform,
            String certPem,
            X509Certificate x509Cert) {
        this(version, cloudKey, cloudProviderIdentifier, cloudPlatform, certPem, x509Cert, null,
                new Date().getTime(), false);
    }

    //CHECKSTYLE_CHECK:OFF ExecutableStatementCount
    public SslCertificateEntry(int version,
            String cloudKey,
            String cloudProviderIdentifier,
            String cloudPlatform,
            String certPem,
            X509Certificate x509Cert,
            String fingerprint,
            long expirationDate,
            boolean deprecated) {
        this.version = version;
        this.cloudKey = requireNonNull(cloudKey);
        this.cloudProviderIdentifier = requireNonNull(cloudProviderIdentifier);
        this.certPem = requireNonNull(certPem);
        this.x509Cert = requireNonNull(x509Cert);
        this.cloudPlatform = requireNonNull(cloudPlatform);
        this.fingerprint = fingerprint;
        this.expirationDate = expirationDate;
        this.deprecated = deprecated;
    }
    //CHECKSTYLE_CHECK:ON ExecutableStatementCount

    /**
     * An implementation of {@link Object#equals(Object)} that decides equality only based on {@code version}.
     *
     * @param o the reference object with which to compare.
     * @return {@code true} if {@code this} object is the same as the {@code o} argument; {@code false} otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SslCertificateEntry that = (SslCertificateEntry) o;
        return version == that.version;
    }

    /**
     * An implementation of {@link Object#hashCode()} that computes the hash code only based on {@code version}.
     *
     * @return a hash code value for {@code this} object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(version);
    }

    @Override
    public String toString() {
        return "SslCertificateEntry{" +
                "version=" + version +
                ", cloudKey='" + cloudKey + '\'' +
                ", cloudProviderIdentifier='" + cloudProviderIdentifier + '\'' +
                ", cloudPlatform='" + cloudPlatform + '\'' +
                ", certPem='" + certPem + '\'' +
                ", x509Cert=" + x509Cert +
                ", fingerprint='" + fingerprint + '\'' +
                ", expirationDate=" + expirationDate +
                ", deprecated=" + deprecated +
                '}';
    }

}
