package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.DatabaseServerModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(Include.NON_NULL)
public class DatabaseServerSslConfig {

    @Schema(description = DatabaseServerModelDescription.SSL_CERTIFICATES)
    private Set<String> sslCertificates;

    @Schema(description = DatabaseServerModelDescription.SSL_CERTIFICATE_TYPE)
    private DatabaseServerSslCertificateType sslCertificateType;

    @Schema(description = DatabaseServerModelDescription.SSL_MODE)
    private DatabaseServerSslMode sslMode;

    public DatabaseServerSslConfig() {
    }

    public Set<String> getSslCertificates() {
        return sslCertificates;
    }

    public void setSslCertificates(Set<String> sslCertificates) {
        this.sslCertificates = sslCertificates;
    }

    public DatabaseServerSslCertificateType getSslCertificateType() {
        return sslCertificateType;
    }

    public void setSslCertificateType(DatabaseServerSslCertificateType sslCertificateType) {
        this.sslCertificateType = sslCertificateType;
    }

    public DatabaseServerSslMode getSslMode() {
        return sslMode;
    }

    public void setSslMode(DatabaseServerSslMode sslMode) {
        this.sslMode = sslMode;
    }

    @Override
    public String toString() {
        return "DatabaseServerSslConfig{" +
                ", sslCertificateType=" + sslCertificateType +
                ", sslMode=" + sslMode +
                '}';
    }
}
