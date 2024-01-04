package com.sequenceiq.cloudbreak.api.endpoint.v4.rotation.requests;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.annotation.ValidSecretType;
import com.sequenceiq.cloudbreak.rotation.request.BaseSecretRotationRequest;
import com.sequenceiq.cloudbreak.validation.ValidCrn;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class StackV4SecretRotationRequest extends BaseSecretRotationRequest {

    @ValidCrn(resource = { CrnResourceDescriptor.DATALAKE, CrnResourceDescriptor.DATAHUB })
    private String crn;

    @ValidSecretType(allowedTypes = CloudbreakSecretType.class)
    @NotNull
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
}
