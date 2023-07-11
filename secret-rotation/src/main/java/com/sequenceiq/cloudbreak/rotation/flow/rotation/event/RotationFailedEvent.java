package com.sequenceiq.cloudbreak.rotation.flow.rotation.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.flow.serialization.RotationEnumDeserializer;
import com.sequenceiq.cloudbreak.rotation.flow.serialization.RotationEnumSerializer;

public class RotationFailedEvent extends RotationEvent {

    private final Exception exception;

    @JsonSerialize(using = RotationEnumSerializer.class)
    @JsonDeserialize(using = RotationEnumDeserializer.class)
    private final SecretRotationStep failedStep;

    @JsonCreator
    public RotationFailedEvent(@JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("secretType") SecretType secretType,
            @JsonProperty("executionType") RotationFlowExecutionType executionType,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("failedStep") SecretRotationStep failedStep) {
        super(selector, resourceId, resourceCrn, secretType, executionType);
        this.exception = exception;
        this.failedStep = failedStep;
    }

    public static RotationFailedEvent fromPayload(String selector, RotationEvent payload, Exception ex, SecretRotationStep failedStep) {
        return new RotationFailedEvent(selector, payload.getResourceId(), payload.getResourceCrn(),
                payload.getSecretType(), payload.getExecutionType(), ex, failedStep);
    }

    public Exception getException() {
        return exception;
    }

    public SecretRotationStep getFailedStep() {
        return failedStep;
    }
}
