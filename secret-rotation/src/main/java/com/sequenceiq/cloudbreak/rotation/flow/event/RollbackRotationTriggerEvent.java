package com.sequenceiq.cloudbreak.rotation.flow.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.flow.event.EventSelectorUtil;

public class RollbackRotationTriggerEvent extends RotationFailedEvent {

    @JsonCreator
    public RollbackRotationTriggerEvent(@JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("secretType") SecretType secretType,
            @JsonProperty("executionType") RotationFlowExecutionType executionType,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("failedStep") SecretRotationStep failedStep) {
        super(selector, resourceId, resourceCrn, secretType, executionType, exception, failedStep);
    }

    public static RollbackRotationTriggerEvent fromPayload(ExecuteRotationFailedEvent payload) {
        return new RollbackRotationTriggerEvent(EventSelectorUtil.selector(RollbackRotationTriggerEvent.class), payload.getResourceId(),
                payload.getResourceCrn(), payload.getSecretType(), payload.getExecutionType(), payload.getException(), payload.getFailedStep());
    }
}
