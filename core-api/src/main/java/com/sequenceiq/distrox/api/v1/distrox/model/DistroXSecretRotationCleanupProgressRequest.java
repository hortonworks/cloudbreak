package com.sequenceiq.distrox.api.v1.distrox.model;

import jakarta.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.annotation.ValidSecretType;
import com.sequenceiq.cloudbreak.validation.ValidCrn;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class DistroXSecretRotationCleanupProgressRequest {

    @ResourceCrn
    @ValidCrn(resource = CrnResourceDescriptor.DATAHUB)
    private String crn;

    @ValidSecretType(allowedTypes = { CloudbreakSecretType.class })
    @NotEmpty
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
        return "DistroXSecretRotationCleanupProgressRequest{" +
                "crn='" + crn + '\'' +
                ", secret='" + secret + '\'' +
                '}';
    }
}
