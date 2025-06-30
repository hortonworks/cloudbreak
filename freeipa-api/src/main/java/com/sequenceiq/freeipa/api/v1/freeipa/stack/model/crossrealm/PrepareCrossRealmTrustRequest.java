package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm;

import static com.sequenceiq.freeipa.api.v2.freeipa.doc.FreeIpaV2ModelDescriptions.FQDN;
import static com.sequenceiq.freeipa.api.v2.freeipa.doc.FreeIpaV2ModelDescriptions.IP;
import static com.sequenceiq.freeipa.api.v2.freeipa.doc.FreeIpaV2ModelDescriptions.REALM;
import static com.sequenceiq.freeipa.api.v2.freeipa.doc.FreeIpaV2ModelDescriptions.TRUST_SECRET;

import jakarta.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PrepareCrossRealmTrustRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrepareCrossRealmTrustRequest extends PrepareCrossRealmTrustBase {
    @NotEmpty
    @Schema(description = FQDN, requiredMode = Schema.RequiredMode.REQUIRED)
    private String fqdn;

    @NotEmpty
    @Schema(description = IP, requiredMode = Schema.RequiredMode.REQUIRED)
    private String ip;

    @NotEmpty
    @Schema(description = REALM, requiredMode = Schema.RequiredMode.REQUIRED)
    private String realm;

    @Schema(description = TRUST_SECRET, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
