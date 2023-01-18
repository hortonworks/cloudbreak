package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.credential;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class CredentialBase {

    private String name;

    private String cloudPlatform;

    private SecretResponse secret;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public SecretResponse getSecret() {
        return secret;
    }

    public void setSecret(SecretResponse secret) {
        this.secret = secret;
    }
}
