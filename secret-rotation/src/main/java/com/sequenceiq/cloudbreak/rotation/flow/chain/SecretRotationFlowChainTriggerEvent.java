package com.sequenceiq.cloudbreak.rotation.flow.chain;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.flow.serialization.RotationEnumListDeserializer;
import com.sequenceiq.cloudbreak.rotation.flow.serialization.RotationEnumListSerializer;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;

public class SecretRotationFlowChainTriggerEvent extends BaseFlowEvent {

    @JsonSerialize(using = RotationEnumListSerializer.class)
    @JsonDeserialize(using = RotationEnumListDeserializer.class)
    private final List<SecretType> secretTypes;

    private final RotationFlowExecutionType executionType;

    private final Map<String, String> additionalProperties;

    @JsonCreator
    public SecretRotationFlowChainTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("secretTypes") List<SecretType> secretTypes,
            @JsonProperty("executionType") RotationFlowExecutionType executionType,
            @JsonProperty("additionalProperties") Map<String, String> additionalProperties) {
        super(selector, resourceId, resourceCrn);
        this.secretTypes = secretTypes;
        this.executionType = executionType;
        this.additionalProperties = additionalProperties;
    }

    public List<SecretType> getSecretTypes() {
        return secretTypes;
    }

    public RotationFlowExecutionType getExecutionType() {
        return executionType;
    }

    public Map<String, String> getAdditionalProperties() {
        return additionalProperties;
    }

    @Override
    public boolean equalsEvent(BaseFlowEvent other) {
        return isClassAndEqualsEvent(SecretRotationFlowChainTriggerEvent.class, other,
                event -> Objects.equals(secretTypes, event.secretTypes) &&
                        Objects.equals(executionType, event.executionType) &&
                        Objects.equals(additionalProperties, event.additionalProperties));
    }

    @Override
    public String toString() {
        return "SecretRotationFlowChainTriggerEvent{" +
                "secretTypes=" + secretTypes +
                ", executionType=" + executionType +
                ", resourceId=" + getResourceId() +
                ", resourceCrn='" + getResourceCrn() +
                ", additionalProperties='" + getAdditionalProperties() +
                '}';
    }
}
