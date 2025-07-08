package com.sequenceiq.environment.api.v1.environment.model.request;

import static com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription.REMOTE_ENVIRONMENT_CRN;
import static com.sequenceiq.freeipa.api.v2.freeipa.doc.FreeIpaV2ModelDescriptions.FQDN;
import static com.sequenceiq.freeipa.api.v2.freeipa.doc.FreeIpaV2ModelDescriptions.IP;
import static com.sequenceiq.freeipa.api.v2.freeipa.doc.FreeIpaV2ModelDescriptions.REALM;
import static com.sequenceiq.freeipa.api.v2.freeipa.doc.FreeIpaV2ModelDescriptions.TRUST_SECRET;

import jakarta.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SetupCrossRealmTrustV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SetupCrossRealmTrustRequest {

    @NotEmpty
    @Schema(description = FQDN, requiredMode = Schema.RequiredMode.REQUIRED)
    private String fqdn;

    @NotEmpty
    @Schema(description = IP, requiredMode = Schema.RequiredMode.REQUIRED)
    private String ip;

    @NotEmpty
    @Schema(description = REALM, requiredMode = Schema.RequiredMode.REQUIRED)
    private String realm;

    @NotEmpty
    @Schema(description = REMOTE_ENVIRONMENT_CRN, requiredMode = Schema.RequiredMode.REQUIRED)
    private String remoteEnvironmentCrn;

    @Schema(description = TRUST_SECRET)
    private String trustSecret;

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

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getRemoteEnvironmentCrn() {
        return remoteEnvironmentCrn;
    }

    public void setRemoteEnvironmentCrn(String remoteEnvironmentCrn) {
        this.remoteEnvironmentCrn = remoteEnvironmentCrn;
    }

    public String getTrustSecret() {
        return trustSecret;
    }

    public void setTrustSecret(String trustSecret) {
        this.trustSecret = trustSecret;
    }

    @Override
    public String toString() {
        return "SetupCrossRealmTrustRequest{" +
                "fqdn='" + fqdn + '\'' +
                ", ip='" + ip + '\'' +
                ", realm='" + realm + '\'' +
                ", remoteEnvironmentCrn='" + remoteEnvironmentCrn + '\'' +
                '}';
    }
}
