package com.sequenceiq.flow.rotation.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;
import com.sequenceiq.flow.rotation.RotationFlowContext;

public class RotationEvent extends BaseFlowEvent {

    private final SecretType secretType;

    private final RotationFlowExecutionType executionType;

    @JsonCreator
    public RotationEvent(@JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("secretType") SecretType secretType,
            @JsonProperty("executionType") RotationFlowExecutionType executionType,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(selector, resourceId, resourceCrn, accepted);
        this.secretType = secretType;
        this.executionType = executionType;
    }

    public RotationEvent(String selector, Long resourceId, String resourceCrn, SecretType secretType, RotationFlowExecutionType executionType) {
        this(selector, resourceId, resourceCrn, secretType, executionType, null);
    }

    public static RotationEvent fromContext(String selector, RotationFlowContext context) {
        return new RotationEvent(selector, context.getResourceId(),
                context.getResourceCrn(), context.getSecretType(), context.getExecutionType(), null);
    }

    public SecretType getSecretType() {
        return secretType;
    }

    public RotationFlowExecutionType getExecutionType() {
        return executionType;
    }

    @Override
    public boolean equalsEvent(BaseFlowEvent other) {
        return isClassAndEqualsEvent(RotationEvent.class, other,
                event -> Objects.equals(secretType, event.secretType) &&
                        Objects.equals(executionType, event.executionType));
    }

    @Override
    public String toString() {
        return "RotationEvent{" +
                "secretType=" + secretType +
                ", executionType=" + executionType +
                ", resourceId=" + getResourceId() +
                ", resourceCrn='" + getResourceCrn() +
                '}';
    }
}
