package com.sequenceiq.cloudbreak.rotation.request;

import java.util.Map;

import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;

import io.swagger.v3.oas.annotations.media.Schema;

public abstract class BaseSecretRotationRequest {

    @Schema(description = "Execution type if needed")
    private RotationFlowExecutionType executionType;

    @Schema(description = "Additional parameters for rotation")
    private Map<String, String> additionalProperties;

    public RotationFlowExecutionType getExecutionType() {
        return executionType;
    }

    public void setExecutionType(RotationFlowExecutionType executionType) {
        this.executionType = executionType;
    }

    public Map<String, String> getAdditionalProperties() {
        return additionalProperties;
    }

    public void setAdditionalProperties(Map<String, String> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }
}
