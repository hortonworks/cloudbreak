package com.sequenceiq.freeipa.api.v2.freeipa.stack.model.crossrealm;

import jakarta.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PrepareCrossRealmTrustV2KdcServerRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrepareCrossRealmTrustV2KdcServerRequest {

    @NotEmpty
    @Schema(description = FreeIpaModelDescriptions.CrossRealmTrustModelDescriptions.KDC_FQDN, requiredMode = Schema.RequiredMode.REQUIRED)
    private String fqdn;

    @NotEmpty
    @Schema(description = FreeIpaModelDescriptions.CrossRealmTrustModelDescriptions.KDC_IP, requiredMode = Schema.RequiredMode.REQUIRED)
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
        return "PrepareCrossRealmTrustKdcServerRequest{" +
                "fqdn='" + fqdn + '\'' +
                ", ip='" + ip + '\'' +
                '}';
    }
}
