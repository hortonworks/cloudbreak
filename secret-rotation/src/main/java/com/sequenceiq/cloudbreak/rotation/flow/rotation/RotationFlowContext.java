package com.sequenceiq.cloudbreak.rotation.flow.rotation;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.flow.rotation.event.RotationEvent;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class RotationFlowContext extends CommonContext {

    private final Long resourceId;

    private final String resourceCrn;

    private final SecretType secretType;

    private final RotationFlowExecutionType executionType;

    private final Map<String, String> additionalProperties;

    @JsonCreator
    public RotationFlowContext(
            @JsonProperty("flowParameters") FlowParameters flowParameters,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("secretType") SecretType secretType,
            @JsonProperty("executionType") RotationFlowExecutionType executionType,
            @JsonProperty("additionalProperties") Map<String, String> additionalProperties) {
        super(flowParameters);
        this.resourceId = resourceId;
        this.resourceCrn = resourceCrn;
        this.secretType = secretType;
        this.executionType = executionType;
        this.additionalProperties = additionalProperties;
    }

    public static RotationFlowContext fromPayload(FlowParameters flowParameters, RotationEvent rotationEvent) {
        return new RotationFlowContext(flowParameters, rotationEvent.getResourceId(), rotationEvent.getResourceCrn(),
                rotationEvent.getSecretType(), rotationEvent.getExecutionType(), rotationEvent.getAdditionalProperties());
    }

    public Long getResourceId() {
        return resourceId;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public SecretType getSecretType() {
        return secretType;
    }

    public RotationFlowExecutionType getExecutionType() {
        return executionType;
    }

    public Map<String, String> getAdditionalProperties() {
        return additionalProperties;
    }

    @Override
    public String toString() {
        return "RotationFlowContext{" +
                "resourceId=" + resourceId +
                ", resourceCrn='" + resourceCrn + '\'' +
                ", secretType=" + secretType +
                ", executionType=" + executionType +
                ", additionalProperties=" + additionalProperties +
                "} " + super.toString();
    }
}
