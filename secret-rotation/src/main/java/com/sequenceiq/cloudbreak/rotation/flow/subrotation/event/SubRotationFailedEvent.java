package com.sequenceiq.cloudbreak.rotation.flow.subrotation.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.flow.event.EventSelectorUtil;

public class SubRotationFailedEvent extends SubRotationEvent {

    private final Exception exception;

    @JsonCreator
    public SubRotationFailedEvent(@JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("secretType") SecretType secretType,
            @JsonProperty("executionType") RotationFlowExecutionType executionType,
            @JsonProperty("exception") Exception exception) {
        super(selector, resourceId, resourceCrn, secretType, executionType);
        this.exception = exception;
    }

    public static SubRotationFailedEvent fromPayload(SubRotationEvent payload, Exception ex) {
        return new SubRotationFailedEvent(EventSelectorUtil.selector(SubRotationFailedEvent.class), payload.getResourceId(), payload.getResourceCrn(),
                payload.getSecretType(), payload.getExecutionType(), ex);
    }

    public Exception getException() {
        return exception;
    }

    @Override
    public String toString() {
        return "SubRotationFailedEvent{" +
                "exception=" + exception +
                "} " + super.toString();
    }
}
