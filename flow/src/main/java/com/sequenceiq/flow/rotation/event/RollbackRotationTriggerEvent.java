package com.sequenceiq.flow.rotation.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;
import com.sequenceiq.flow.event.EventSelectorUtil;

public class RollbackRotationTriggerEvent extends RotationFailedEvent {

    @JsonCreator
    public RollbackRotationTriggerEvent(@JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("secretType") SecretType secretType,
            @JsonProperty("executionType") RotationFlowExecutionType executionType,
            @JsonProperty("exception") Exception exception) {
        super(selector, resourceId, resourceCrn, secretType, executionType, exception);
    }

    public static RollbackRotationTriggerEvent fromPayload(RotationEvent payload, Exception exception) {
        return new RollbackRotationTriggerEvent(EventSelectorUtil.selector(RollbackRotationTriggerEvent.class),
                payload.getResourceId(), payload.getResourceCrn(), payload.getSecretType(), payload.getExecutionType(), exception);
    }
}
