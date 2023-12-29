package com.sequenceiq.cloudbreak.cloud.model.database;

import static java.util.Objects.requireNonNull;

import java.util.Set;

/**
 * Represents a set of SSL certificates (realized as {@link CloudDatabaseServerSslCertificate} instances) for a cloud provider managed database server.
 */
public record CloudDatabaseServerSslCertificates(Set<CloudDatabaseServerSslCertificate> sslCertificates) {

    /**
     * Creates a new {@code CloudDatabaseServerSslCertificates} using the given arguments.
     *
     * @param sslCertificates set of {@link CloudDatabaseServerSslCertificate} instances; must not be {@code null} but may be empty
     * @throws NullPointerException if {@code sslCertificates == null}
     */
    public CloudDatabaseServerSslCertificates(Set<CloudDatabaseServerSslCertificate> sslCertificates) {
        this.sslCertificates = requireNonNull(sslCertificates);
    }

}
