package com.sequenceiq.cloudbreak.cloud.model.database;

import static java.util.Objects.requireNonNull;


/**
 * Represents an SSL certificate for a cloud provider database server. Overrides both {@link #equals(Object)} and {@link #hashCode()}, thus instances of
 * this class can be also used as collection elements.
 */
public record CloudDatabaseServerSslCertificate(CloudDatabaseServerSslCertificateType certificateType, String certificateIdentifier, String certificate) {

    /**
     * Creates a new {@code CloudDatabaseServerSslCertificate} instance using the given arguments.
     *
     * @param certificateType       {@link CloudDatabaseServerSslCertificateType} instance representing SSL certificate type; must not be {@code null}
     * @param certificateIdentifier cloud provider specific identifier of the SSL certificate; must not be {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    public CloudDatabaseServerSslCertificate(CloudDatabaseServerSslCertificateType certificateType, String certificateIdentifier) {
        this(certificateType, certificateIdentifier, null);
    }

    /**
     * Creates a new {@code CloudDatabaseServerSslCertificate} instance using the given arguments.
     *
     * @param certificateType       {@link CloudDatabaseServerSslCertificateType} instance representing SSL certificate type; must not be {@code null}
     * @param certificateIdentifier cloud provider specific identifier of the SSL certificate; must not be {@code null}
     * @param certificate           the certificate itself, it can be empty or null
     * @throws NullPointerException if either argument is {@code null}
     */
    public CloudDatabaseServerSslCertificate(CloudDatabaseServerSslCertificateType certificateType, String certificateIdentifier, String certificate) {
        this.certificateType = requireNonNull(certificateType);
        this.certificateIdentifier = requireNonNull(certificateIdentifier);
        this.certificate = certificate;
    }

    /**
     * Retrieves the SSL certificate type.
     *
     * @return {@link CloudDatabaseServerSslCertificateType} instance; never {@code null}
     */
    @Override
    public CloudDatabaseServerSslCertificateType certificateType() {
        return certificateType;
    }

    /**
     * Retrieves the cloud provider specific identifier of the SSL certificate.
     *
     * @return cloud provider specific identifier; never {@code null}
     */
    @Override
    public String certificateIdentifier() {
        return certificateIdentifier;
    }

    /**
     * @return the certificate or null if it's absent
     */
    @Override
    public String certificate() {
        return certificate;
    }

    @Override
    public String toString() {
        return "DatabaseSslCertificate{" +
                "certificateType=" + certificateType +
                ", certificateIdentifier='" + certificateIdentifier + '\'' +
                '}';
    }

}
