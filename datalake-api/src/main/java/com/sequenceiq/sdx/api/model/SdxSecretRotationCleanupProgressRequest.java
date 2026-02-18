package com.sequenceiq.sdx.api.model;

import jakarta.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.rotation.annotation.ValidSecretType;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.sdx.rotation.DatalakeSecretType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxSecretRotationCleanupProgressRequest {

    @ResourceCrn
    @ValidCrn(resource = CrnResourceDescriptor.VM_DATALAKE)
    @Schema(description = ModelDescriptions.DATA_LAKE_CRN)
    private String crn;

    @ValidSecretType(allowedTypes = { DatalakeSecretType.class })
    @NotEmpty
    @Schema(description = "Secret to be cleaned up in progress database")
    private String secret;

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    @Override
    public String toString() {
        return "SdxSecretRotationCleanupProgressRequest{" +
                "crn='" + crn + '\'' +
                ", secret='" + secret + '\'' +
                '}';
    }
}
