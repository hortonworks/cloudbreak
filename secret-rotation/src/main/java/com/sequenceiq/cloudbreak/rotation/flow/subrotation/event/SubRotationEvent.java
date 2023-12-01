package com.sequenceiq.cloudbreak.rotation.flow.subrotation.event;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.flow.serialization.RotationEnumDeserializer;
import com.sequenceiq.cloudbreak.rotation.flow.serialization.RotationEnumSerializer;
import com.sequenceiq.cloudbreak.rotation.flow.subrotation.SubRotationFlowContext;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;

public class SubRotationEvent extends BaseFlowEvent {

    @JsonSerialize(using = RotationEnumSerializer.class)
    @JsonDeserialize(using = RotationEnumDeserializer.class)
    private final SecretType secretType;

    private final RotationFlowExecutionType executionType;

    private final Map<String, String> additionalProperties;

    @JsonCreator
    public SubRotationEvent(@JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("secretType") SecretType secretType,
            @JsonProperty("executionType") RotationFlowExecutionType executionType,
            @JsonProperty("additionalProperties") Map<String, String> additionalProperties,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(selector, resourceId, resourceCrn, accepted);
        this.secretType = secretType;
        this.executionType = executionType;
        this.additionalProperties = additionalProperties;
    }

    public SubRotationEvent(String selector, Long resourceId, String resourceCrn, SecretType secretType, RotationFlowExecutionType executionType,
            Map<String, String> additionalProperties) {
        this(selector, resourceId, resourceCrn, secretType, executionType, additionalProperties, null);
    }

    public static SubRotationEvent fromContext(String selector, SubRotationFlowContext context) {
        return new SubRotationEvent(selector, context.getResourceId(),
                context.getResourceCrn(), context.getSecretType(), context.getExecutionType(), context.getAdditionalProperties(), null);
    }

    public SecretType getSecretType() {
        return secretType;
    }

    public RotationFlowExecutionType getExecutionType() {
        return executionType;
    }

    public Map<String, String> getAdditionalProperties() {
        return additionalProperties;
    }

    @Override
    public boolean equalsEvent(BaseFlowEvent other) {
        return isClassAndEqualsEvent(SubRotationEvent.class, other,
                event -> Objects.equals(secretType, event.secretType) &&
                        Objects.equals(executionType, event.executionType) &&
                        Objects.equals(additionalProperties, event.additionalProperties));
    }

    @Override
    public String toString() {
        return "SubRotationEvent{" +
                "secretType=" + secretType +
                ", executionType=" + executionType +
                ", resourceId=" + getResourceId() +
                ", resourceCrn='" + getResourceCrn() +
                ", additionalProperties='" + additionalProperties +
                '}';
    }
}
