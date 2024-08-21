package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.common.domain.SslCertStatus;
import com.sequenceiq.redbeams.doc.ModelDescriptions;
import com.sequenceiq.redbeams.doc.ModelDescriptions.DatabaseServer;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.DATABASE_SERVER_CERTIFICATE_RESPONSE)
@JsonInclude(Include.NON_NULL)
public class ClusterDatabaseServerCertificateStatusV4Response {

    @Schema(description = DatabaseServer.ENVIRONMENT_CRN)
    private String crn;

    @Schema(description = DatabaseServer.SSL_CERTIFICATE_STATUS)
    private SslCertStatus sslStatus;

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public SslCertStatus getSslStatus() {
        return sslStatus;
    }

    public void setSslStatus(SslCertStatus sslStatus) {
        this.sslStatus = sslStatus;
    }

    @Override
    public String toString() {
        return "ClusterDatabaseServerCertificateStatusV4Response{" +
                "crn='" + crn + '\'' +
                ", sslStatus=" + sslStatus +
                '}';
    }
}
