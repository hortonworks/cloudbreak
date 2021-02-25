package com.sequenceiq.redbeams.configuration;

import static java.util.Objects.requireNonNull;

import java.security.cert.X509Certificate;
import java.util.Objects;

public class SslCertificateEntry {

    private final int version;

    private final String cloudProviderIdentifier;

    private final String certPem;

    private final X509Certificate x509Cert;

    public SslCertificateEntry(int version, String cloudProviderIdentifier, String certPem, X509Certificate x509Cert) {
        this.version = version;
        this.cloudProviderIdentifier = requireNonNull(cloudProviderIdentifier);
        this.certPem = requireNonNull(certPem);
        this.x509Cert = requireNonNull(x509Cert);
    }

    public int getVersion() {
        return version;
    }

    public String getCloudProviderIdentifier() {
        return cloudProviderIdentifier;
    }

    public String getCertPem() {
        return certPem;
    }

    public X509Certificate getX509Cert() {
        return x509Cert;
    }

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
                ", cloudProviderIdentifier='" + cloudProviderIdentifier + '\'' +
                ", certPem='" + certPem + '\'' +
                ", x509Cert=" + x509Cert +
                '}';
    }

}
