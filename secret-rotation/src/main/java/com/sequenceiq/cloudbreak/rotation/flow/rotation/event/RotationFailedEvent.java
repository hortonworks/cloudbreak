package com.sequenceiq.cloudbreak.rotation.flow.rotation.event;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.flow.event.EventSelectorUtil;

public class RotationFailedEvent extends RotationEvent {

    private final Exception exception;

    private final RotationFlowExecutionType failedAt;

    @JsonCreator
    public RotationFailedEvent(@JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("secretType") SecretType secretType,
            @JsonProperty("executionType") RotationFlowExecutionType executionType,
            @JsonProperty("additionalProperties") Map<String, String> additionalProperties,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("failedAt") RotationFlowExecutionType failedAt) {
        super(selector, resourceId, resourceCrn, secretType, executionType, additionalProperties);
        this.exception = exception;
        this.failedAt = failedAt;
    }

    public static RotationFailedEvent fromPayload(RotationEvent payload, Exception ex, RotationFlowExecutionType failedAt) {
        return new RotationFailedEvent(EventSelectorUtil.selector(RotationFailedEvent.class), payload.getResourceId(), payload.getResourceCrn(),
                payload.getSecretType(), payload.getExecutionType(), payload.getAdditionalProperties(), ex, failedAt);
    }

    public Exception getException() {
        return exception;
    }

    public RotationFlowExecutionType getFailedAt() {
        return failedAt;
    }

    @Override
    public String toString() {
        return "RotationFailedEvent{" +
                "exception=" + exception +
                "} " + super.toString();
    }
}
