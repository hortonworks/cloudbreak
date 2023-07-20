package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.rotate;

import java.util.List;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.validation.ValidSecretTypes;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FreeIpaSecretRotationRequest {

    @ValidSecretTypes
    @NotEmpty
    private List<String> secrets;

    @NotNull
    private RotationFlowExecutionType executionType;

    public List<String> getSecrets() {
        return secrets;
    }

    public void setSecrets(List<String> secrets) {
        this.secrets = secrets;
    }

    public RotationFlowExecutionType getExecutionType() {
        return executionType;
    }

    public void setExecutionType(RotationFlowExecutionType executionType) {
        this.executionType = executionType;
    }

    @Override
    public String toString() {
        return "FreeIpaSecretRotationRequest{" +
                "secrets=" + secrets +
                ", executionType=" + executionType +
                '}';
    }
}
