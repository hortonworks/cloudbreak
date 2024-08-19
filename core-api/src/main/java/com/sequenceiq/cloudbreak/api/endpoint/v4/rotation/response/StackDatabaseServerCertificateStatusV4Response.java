package com.sequenceiq.cloudbreak.api.endpoint.v4.rotation.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.common.domain.SslCertStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response containing information about a database server SSL certificate")
@JsonInclude(Include.NON_NULL)
public class StackDatabaseServerCertificateStatusV4Response {

    @Schema(description = "Crn on the stack")
    private String crn;

    @Schema(description = "Current status of the set of relevant SSL certificates for the database server")
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
        return "StackDatabaseServerCertificateStatusV4Response{" +
                "crn='" + crn + '\'' +
                ", sslStatus=" + sslStatus +
                '}';
    }
}
