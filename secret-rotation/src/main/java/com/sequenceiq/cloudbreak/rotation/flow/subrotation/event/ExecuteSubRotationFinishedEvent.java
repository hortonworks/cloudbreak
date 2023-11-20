package com.sequenceiq.cloudbreak.rotation.flow.subrotation.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.flow.event.EventSelectorUtil;

public class ExecuteSubRotationFinishedEvent extends SubRotationEvent {

    @JsonCreator
    public ExecuteSubRotationFinishedEvent(@JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("secretType") SecretType secretType,
            @JsonProperty("executionType") RotationFlowExecutionType executionType) {
        super(selector, resourceId, resourceCrn, secretType, executionType);
    }

    public static ExecuteSubRotationFinishedEvent fromPayload(SubRotationEvent payload) {
        return new ExecuteSubRotationFinishedEvent(EventSelectorUtil.selector(ExecuteSubRotationFinishedEvent.class),
                payload.getResourceId(), payload.getResourceCrn(), payload.getSecretType(), payload.getExecutionType());
    }

    @Override
    public String toString() {
        return "ExecuteSubRotationFinishedEvent{} " + super.toString();
    }
}
