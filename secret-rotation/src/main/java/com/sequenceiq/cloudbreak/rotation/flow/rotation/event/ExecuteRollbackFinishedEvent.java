package com.sequenceiq.cloudbreak.rotation.flow.rotation.event;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.flow.event.EventSelectorUtil;

public class ExecuteRollbackFinishedEvent extends RotationEvent {

    private final Exception rollbackReason;

    @JsonCreator
    public ExecuteRollbackFinishedEvent(@JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("secretType") SecretType secretType,
            @JsonProperty("executionType") RotationFlowExecutionType executionType,
            @JsonProperty("additionalProperties") Map<String, String> additionalProperties,
            @JsonProperty("rollbackReason") Exception rollbackReason) {
        super(selector, resourceId, resourceCrn, secretType, executionType, additionalProperties);
        this.rollbackReason = rollbackReason;
    }

    public static ExecuteRollbackFinishedEvent fromPayload(RollbackRotationTriggerEvent payload) {
        return new ExecuteRollbackFinishedEvent(EventSelectorUtil.selector(ExecuteRollbackFinishedEvent.class),
                payload.getResourceId(), payload.getResourceCrn(), payload.getSecretType(), payload.getExecutionType(), payload.getAdditionalProperties(),
                payload.getRollbackReason());
    }

    public Exception getRollbackReason() {
        return rollbackReason;
    }

    @Override
    public String toString() {
        return "ExecuteRollbackFinishedEvent{" +
                "rollbackReason=" + rollbackReason +
                "} " + super.toString();
    }
}
