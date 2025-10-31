package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions.CrossRealmTrustModelDescriptions;

import jakarta.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PrepareCrossRealmTrustV1Request", deprecated = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrepareCrossRealmTrustRequest extends PrepareCrossRealmTrustBase {
    @ValidCrn(resource = {CrnResourceDescriptor.ENVIRONMENT, CrnResourceDescriptor.CLASSIC_CLUSTER})
    @Schema(description = ModelDescriptions.REMOTE_ENVIRONMENT_CRN, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String remoteEnvironmentCrn;

    @NotEmpty
    @Schema(description = CrossRealmTrustModelDescriptions.FQDN, requiredMode = Schema.RequiredMode.REQUIRED)
    private String fqdn;

    @NotEmpty
    @Schema(description = CrossRealmTrustModelDescriptions.IP, requiredMode = Schema.RequiredMode.REQUIRED)
    private String ip;

    @NotEmpty
    @Schema(description = CrossRealmTrustModelDescriptions.REALM, requiredMode = Schema.RequiredMode.REQUIRED)
    private String realm;

    @Schema(description = CrossRealmTrustModelDescriptions.TRUST_SECRET, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String trustSecret;

    public String getRemoteEnvironmentCrn() {
        return remoteEnvironmentCrn;
    }

    public void setRemoteEnvironmentCrn(String remoteEnvironmentCrn) {
        this.remoteEnvironmentCrn = remoteEnvironmentCrn;
    }

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

    public @NotEmpty String getRealm() {
        return realm;
    }

    public void setRealm(@NotEmpty String realm) {
        this.realm = realm;
    }

    public String getTrustSecret() {
        return trustSecret;
    }

    public void setTrustSecret(String trustSecret) {
        this.trustSecret = trustSecret;
    }

    @Override
    public String toString() {
        return "PrepareCrossRealmTrustRequest{" +
                "fqdn='" + fqdn + '\'' +
                ", ip='" + ip + '\'' +
                ", realm='" + realm + '\'' +
                "} " + super.toString();
    }
}
