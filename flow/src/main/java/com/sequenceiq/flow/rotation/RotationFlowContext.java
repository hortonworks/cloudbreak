package com.sequenceiq.flow.rotation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.rotation.event.RotationEvent;

public class RotationFlowContext extends CommonContext {

    private Long resourceId;

    private String resourceCrn;

    private SecretType secretType;

    private RotationFlowExecutionType executionType;

    @JsonCreator
    public RotationFlowContext(
            @JsonProperty("flowParameters") FlowParameters flowParameters,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("secretType") SecretType secretType,
            @JsonProperty("executionType") RotationFlowExecutionType executionType) {
        super(flowParameters);
        this.resourceId = resourceId;
        this.resourceCrn = resourceCrn;
        this.secretType = secretType;
        this.executionType = executionType;
    }

    public static RotationFlowContext fromPayload(FlowParameters flowParameters, RotationEvent rotationEvent) {
        return new RotationFlowContext(flowParameters, rotationEvent.getResourceId(), rotationEvent.getResourceCrn(),
                rotationEvent.getSecretType(), rotationEvent.getExecutionType());
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
}
