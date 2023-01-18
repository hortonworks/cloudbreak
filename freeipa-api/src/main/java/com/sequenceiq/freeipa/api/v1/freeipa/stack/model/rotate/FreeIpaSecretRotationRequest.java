package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.rotate;

import java.util.List;

import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.rotation.annotation.ValidSecretTypes;
import com.sequenceiq.cloudbreak.rotation.request.BaseSecretRotationRequest;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FreeIpaSecretRotationRequest extends BaseSecretRotationRequest {

    @ValidSecretTypes(allowedTypes = { FreeIpaSecretType.class })
    @NotEmpty
    private List<String> secrets;

    public List<String> getSecrets() {
        return secrets;
    }

    public void setSecrets(List<String> secrets) {
        this.secrets = secrets;
    }

    @Override
    public String toString() {
        return "FreeIpaSecretRotationRequest{" +
                "secrets=" + secrets +
                ", executionType=" + getExecutionType() +
                ", additionalProperties=" + getAdditionalProperties() +
                '}';
    }
}
