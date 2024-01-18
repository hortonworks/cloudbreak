package com.sequenceiq.cloudbreak.cloud.model.database;

import static java.util.Objects.requireNonNull;

import org.apache.commons.lang3.StringUtils;

/**
 * Represents an SSL certificate for a cloud provider managed database server.
 */
public record CloudDatabaseServerSslCertificate(CloudDatabaseServerSslCertificateType certificateType, String certificateIdentifier, String certificate,
        Long expirationDate, boolean overridden) {

    /**
     * Creates a new {@code CloudDatabaseServerSslCertificate} instance using the given arguments, and assuming {@code certificate = null} and
     * {@code overridden = false}.
     *
     * @param certificateType       {@link CloudDatabaseServerSslCertificateType} instance representing SSL certificate type; must not be {@code null}
     * @param certificateIdentifier cloud provider specific identifier of the SSL certificate; must not be {@code null} or blank
     * @throws NullPointerException if either argument is {@code null}
     * @throws IllegalArgumentException if {@code certificateIdentifier} is blank
     */
    public CloudDatabaseServerSslCertificate(CloudDatabaseServerSslCertificateType certificateType, String certificateIdentifier) {
        this(certificateType, certificateIdentifier, null, 0L);
    }

    /**
     * Creates a new {@code CloudDatabaseServerSslCertificate} instance using the given arguments, and assuming {@code overridden = false}.
     *
     * @param certificateType       {@link CloudDatabaseServerSslCertificateType} instance representing SSL certificate type; must not be {@code null}
     * @param certificateIdentifier cloud provider specific identifier of the SSL certificate; must not be {@code null} or blank
     * @param certificate           the certificate PEM payload itself, it can be empty or {@code null}
     * @throws NullPointerException if either {@code certificateType} or {@code certificateIdentifier} is {@code null}
     * @throws IllegalArgumentException if {@code certificateIdentifier} is blank
     */
    public CloudDatabaseServerSslCertificate(CloudDatabaseServerSslCertificateType certificateType, String certificateIdentifier, String certificate,
        long expirationDate) {
        this(certificateType, certificateIdentifier, certificate, expirationDate, false);
    }

    /**
     * Creates a new {@code CloudDatabaseServerSslCertificate} instance using the given arguments, and assuming {@code certificate = null}.
     *
     * @param certificateType       {@link CloudDatabaseServerSslCertificateType} instance representing SSL certificate type; must not be {@code null}
     * @param certificateIdentifier cloud provider specific identifier of the SSL certificate; must not be {@code null} or blank
     * @param overridden            {@code true} if this certificate has been marked as preferred by the user, thus overriding the system default setting
     * @throws NullPointerException if either {@code certificateType} or {@code certificateIdentifier} is {@code null}
     * @throws IllegalArgumentException if {@code certificateIdentifier} is blank
     */
    public CloudDatabaseServerSslCertificate(CloudDatabaseServerSslCertificateType certificateType, String certificateIdentifier, boolean overridden) {
        this(certificateType, certificateIdentifier, null, null, overridden);
    }

    /**
     * Creates a new {@code CloudDatabaseServerSslCertificate} instance using the given arguments.
     *
     * @param certificateType       {@link CloudDatabaseServerSslCertificateType} instance representing SSL certificate type; must not be {@code null}
     * @param certificateIdentifier cloud provider specific identifier of the SSL certificate; must not be {@code null} or blank
     * @param certificate           the certificate PEM payload itself, it can be empty or {@code null}
     * @param overridden            {@code true} if this certificate has been marked as preferred by the user, thus overriding the system default setting
     * @throws NullPointerException if either {@code certificateType} or {@code certificateIdentifier} is {@code null}
     * @throws IllegalArgumentException if {@code certificateIdentifier} is blank
     */
    public CloudDatabaseServerSslCertificate(CloudDatabaseServerSslCertificateType certificateType, String certificateIdentifier, String certificate,
            Long expirationDate, boolean overridden) {
        this.certificateType = requireNonNull(certificateType);
        this.certificateIdentifier = requireNonNull(certificateIdentifier);
        if (StringUtils.isBlank(certificateIdentifier)) {
            throw new IllegalArgumentException("certificateIdentifier must not be blank");
        }
        this.certificate = certificate;
        this.overridden = overridden;
        this.expirationDate = expirationDate;
    }

}
