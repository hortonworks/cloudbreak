package com.sequenceiq.flow.rotation.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;

public class FinalizeRotationSuccessEvent extends RotationEvent {

    @JsonCreator
    public FinalizeRotationSuccessEvent(@JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("secretType") SecretType secretType,
            @JsonProperty("executionType") RotationFlowExecutionType executionType) {
        super(selector, resourceId, resourceCrn, secretType, executionType);
    }

    public static FinalizeRotationSuccessEvent fromPayload(String selector, RotationEvent payload) {
        return new FinalizeRotationSuccessEvent(selector, payload.getResourceId(), payload.getResourceCrn(),
                payload.getSecretType(), payload.getExecutionType());
    }
}
