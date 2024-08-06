package com.sequenceiq.environment.api.v1.environment.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.common.domain.SslCertStatus;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = EnvironmentModelDescription.DATABASE_SERVER_CERTIFICATE_RESPONSE)
@JsonInclude(Include.NON_NULL)
public class EnvironmentDatabaseServerCertificateStatusV4Response {

    @Schema(description = EnvironmentModelDescription.ENVIRONMENT_CRN)
    private String environmentCrn;

    @Schema(description = EnvironmentModelDescription.SSL_CERTIFICATE_STATUS)
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
