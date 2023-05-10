package com.sequenceiq.flow.rotation.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;

public class RotationFailedEvent extends RotationEvent {

    private final Exception exception;

    @JsonCreator
    public RotationFailedEvent(@JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("secretType") SecretType secretType,
            @JsonProperty("executionType") RotationFlowExecutionType executionType,
            @JsonProperty("exception") Exception exception) {
        super(selector, resourceId, resourceCrn, secretType, executionType);
        this.exception = exception;
    }

    public static RotationFailedEvent fromPayload(String selector, RotationEvent payload, Exception ex) {
        return new RotationFailedEvent(selector, payload.getResourceId(), payload.getResourceCrn(),
                payload.getSecretType(), payload.getExecutionType(), ex);
    }

    public Exception getException() {
        return exception;
    }
}
