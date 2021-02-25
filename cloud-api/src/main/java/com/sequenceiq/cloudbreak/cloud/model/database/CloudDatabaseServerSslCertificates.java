package com.sequenceiq.cloudbreak.cloud.model.database;

import java.util.Objects;
import java.util.Set;

/**
 * Represents a set of SSL certificates (realized as {@link CloudDatabaseServerSslCertificate} instances) for a cloud provider database server.
 * Overrides both {@link #equals(Object)} and {@link #hashCode()}, thus instances of this class can be also used as collection elements.
 */
public class CloudDatabaseServerSslCertificates {

    private final Set<CloudDatabaseServerSslCertificate> sslCertificates;

    /**
     * Creates a new {@code CloudDatabaseServerSslCertificates} using the given arguments.
     * @param sslCertificates set of {@link CloudDatabaseServerSslCertificate} instances; must not be {@code null} but may be empty
     * @throws NullPointerException if {@code sslCertificates == null}
     */
    public CloudDatabaseServerSslCertificates(Set<CloudDatabaseServerSslCertificate> sslCertificates) {
        this.sslCertificates = Objects.requireNonNull(sslCertificates);
    }

    /**
     * Retrieves the set of SSL certificates.
     * @return set of SSL certificates; never {@code null} but may be empty
     */
    public Set<CloudDatabaseServerSslCertificate> getSslCertificates() {
        return sslCertificates;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CloudDatabaseServerSslCertificates that = (CloudDatabaseServerSslCertificates) o;
        return sslCertificates.equals(that.sslCertificates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sslCertificates);
    }

    @Override
    public String toString() {
        return "DatabaseSslCertificates{" +
                "sslCertificates=" + sslCertificates +
                '}';
    }

}
