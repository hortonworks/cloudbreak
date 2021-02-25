package com.sequenceiq.cloudbreak.cloud.model.database;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

/**
 * Represents an SSL certificate for a cloud provider database server. Overrides both {@link #equals(Object)} and {@link #hashCode()}, thus instances of
 * this class can be also used as collection elements.
 */
public class CloudDatabaseServerSslCertificate {

    private final CloudDatabaseServerSslCertificateType certificateType;

    private final String certificateIdentifier;

    /**
     * Creates a new {@code CloudDatabaseServerSslCertificate} instance using the given arguments.
     * @param certificateType {@link CloudDatabaseServerSslCertificateType} instance representing SSL certificate type; must not be {@code null}
     * @param certificateIdentifier cloud provider specific identifier of the SSL certificate; must not be {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    public CloudDatabaseServerSslCertificate(CloudDatabaseServerSslCertificateType certificateType, String certificateIdentifier) {
        this.certificateType = requireNonNull(certificateType);
        this.certificateIdentifier = requireNonNull(certificateIdentifier);
    }

    /**
     * Retrieves the SSL certificate type.
     * @return {@link CloudDatabaseServerSslCertificateType} instance; never {@code null}
     */
    public CloudDatabaseServerSslCertificateType getCertificateType() {
        return certificateType;
    }

    /**
     * Retrieves the cloud provider specific identifier of the SSL certificate.
     * @return cloud provider specific identifier; never {@code null}
     */
    public String getCertificateIdentifier() {
        return certificateIdentifier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CloudDatabaseServerSslCertificate that = (CloudDatabaseServerSslCertificate) o;
        return certificateType == that.certificateType && certificateIdentifier.equals(that.certificateIdentifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(certificateType, certificateIdentifier);
    }

    @Override
    public String toString() {
        return "DatabaseSslCertificate{" +
                "certificateType=" + certificateType +
                ", certificateIdentifier='" + certificateIdentifier + '\'' +
                '}';
    }

}
