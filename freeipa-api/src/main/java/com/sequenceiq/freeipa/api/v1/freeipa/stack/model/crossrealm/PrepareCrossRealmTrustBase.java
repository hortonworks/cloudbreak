package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm;

import static com.sequenceiq.freeipa.api.v2.freeipa.doc.FreeIpaV2ModelDescriptions.FQDN;
import static com.sequenceiq.freeipa.api.v2.freeipa.doc.FreeIpaV2ModelDescriptions.IP;
import static com.sequenceiq.freeipa.api.v2.freeipa.doc.FreeIpaV2ModelDescriptions.REALM;

import jakarta.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrepareCrossRealmTrustBase {
    @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT)
    @NotEmpty
    @Schema(description = ModelDescriptions.ENVIRONMENT_CRN, requiredMode = Schema.RequiredMode.REQUIRED)
    private String environmentCrn;

    @ValidCrn(resource = CrnResourceDescriptor.FREEIPA)
    @NotEmpty
    @Schema(description = ModelDescriptions.CRN, requiredMode = Schema.RequiredMode.REQUIRED)
    private String resourceCrn;

    @NotEmpty
    @Schema(description = FQDN, requiredMode = Schema.RequiredMode.REQUIRED)
    private String fqdn;

    @NotEmpty
    @Schema(description = IP, requiredMode = Schema.RequiredMode.REQUIRED)
    private String ip;

    @NotEmpty
    @Schema(description = REALM, requiredMode = Schema.RequiredMode.REQUIRED)
    private String realm;

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
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

    @Override
    public String toString() {
        return "PrepareCrossRealmTrustBase{" +
                "environmentCrn='" + environmentCrn + '\'' +
                ", resourceCrn='" + resourceCrn + '\'' +
                ", fqdn='" + fqdn + '\'' +
                ", ip='" + ip + '\'' +
                '}';
    }
}
