package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.rotate;

import jakarta.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.rotation.annotation.ValidSecretType;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FreeIpaSecretRotationCleanupProgressRequest {

    @ResourceCrn
    @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT)
    private String environmentCrn;

    @ValidSecretType(allowedTypes = { FreeIpaSecretType.class })
    @NotEmpty
    private String secret;

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    @Override
    public String toString() {
        return "FreeIpaSecretRotationRequest{" +
                "secret=" + secret +
                ", environmentCrn=" + environmentCrn +
                '}';
    }
}
