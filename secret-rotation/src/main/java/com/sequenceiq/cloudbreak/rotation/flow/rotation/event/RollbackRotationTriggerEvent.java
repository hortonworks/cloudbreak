package com.sequenceiq.cloudbreak.rotation.flow.rotation.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.flow.event.EventSelectorUtil;

public class RollbackRotationTriggerEvent extends RotationEvent {

    private final Exception rollbackReason;

    @JsonCreator
    public RollbackRotationTriggerEvent(@JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("secretType") SecretType secretType,
            @JsonProperty("executionType") RotationFlowExecutionType executionType,
            @JsonProperty("rollbackReason") Exception rollbackReason) {
        super(selector, resourceId, resourceCrn, secretType, executionType);
        this.rollbackReason = rollbackReason;
    }

    public static RollbackRotationTriggerEvent fromPayload(ExecuteRotationFailedEvent payload) {
        return new RollbackRotationTriggerEvent(EventSelectorUtil.selector(RollbackRotationTriggerEvent.class), payload.getResourceId(),
                payload.getResourceCrn(), payload.getSecretType(), payload.getExecutionType(), payload.getRollbackReason());
    }

    public Exception getRollbackReason() {
        return rollbackReason;
    }

    @Override
    public String toString() {
        return "RollbackRotationTriggerEvent{" +
                "rollbackReason=" + rollbackReason +
                "} " + super.toString();
    }
}
