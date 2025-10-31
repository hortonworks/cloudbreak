package com.sequenceiq.environment.api.v2.environment.model.request;

import jakarta.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions.CrossRealmTrustModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SetupCrossRealmTrustV2KdcServerRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SetupCrossRealmTrustV2KdcServerRequest {

    @NotEmpty
    @Schema(description = CrossRealmTrustModelDescriptions.KDC_FQDN, requiredMode = Schema.RequiredMode.REQUIRED)
    private String fqdn;

    @NotEmpty
    @Schema(description = CrossRealmTrustModelDescriptions.KDC_IP, requiredMode = Schema.RequiredMode.REQUIRED)
    private String ip;

    public String getFqdn() {
        return fqdn;
    }

    public void setFqdn(String fqdn) {
        this.fqdn = fqdn;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    @Override
    public String toString() {
        return "SetupCrossRealmTrustKdcServerRequest{" +
                "fqdn='" + fqdn + '\'' +
                ", ip='" + ip + '\'' +
                '}';
    }
}
