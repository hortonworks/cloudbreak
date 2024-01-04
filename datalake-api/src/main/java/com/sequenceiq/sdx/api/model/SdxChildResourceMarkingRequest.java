package com.sequenceiq.sdx.api.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.rotation.annotation.ValidMultiSecretType;
import com.sequenceiq.cloudbreak.validation.ValidCrn;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxChildResourceMarkingRequest {

    @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT)
    @Schema(description = ModelDescriptions.ENVIRONMENT_CRN)
    @NotNull
    private String parentCrn;

    @ValidMultiSecretType
    @NotEmpty
    @Schema(description = "Secret to be rotated")
    private String secret;

    public String getParentCrn() {
        return parentCrn;
    }

    public void setParentCrn(String parentCrn) {
        this.parentCrn = parentCrn;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
