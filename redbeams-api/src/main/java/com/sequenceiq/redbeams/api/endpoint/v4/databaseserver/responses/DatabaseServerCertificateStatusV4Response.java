package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslCertStatus;
import com.sequenceiq.redbeams.doc.ModelDescriptions;
import com.sequenceiq.redbeams.doc.ModelDescriptions.DatabaseServer;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.DATABASE_SERVER_CERTIFICATE_RESPONSE)
@JsonInclude(Include.NON_NULL)
public class DatabaseServerCertificateStatusV4Response {

    @Schema(description = DatabaseServer.ENVIRONMENT_CRN)
    private String environmentCrn;

    @Schema(description = DatabaseServer.SSL_CERTIFICATE_STATUS)
    private SslCertStatus sslStatus;

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public SslCertStatus getSslStatus() {
        return sslStatus;
    }

    public void setSslStatus(SslCertStatus sslStatus) {
        this.sslStatus = sslStatus;
    }

    @Override
    public String toString() {
        return "DatabaseServerV4Response{" +
                "environmentCrn='" + environmentCrn + '\'' +
                ", sslStatus=" + sslStatus +
                '}';
    }
}
