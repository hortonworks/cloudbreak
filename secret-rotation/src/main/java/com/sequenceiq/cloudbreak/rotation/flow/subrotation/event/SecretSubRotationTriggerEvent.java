package com.sequenceiq.cloudbreak.rotation.flow.subrotation.event;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.flow.chain.SecretRotationFlowChainTriggerEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public class SecretSubRotationTriggerEvent extends SubRotationEvent {

    @JsonCreator
    public SecretSubRotationTriggerEvent(@JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("secretType") SecretType secretType,
            @JsonProperty("executionType") RotationFlowExecutionType executionType,
            @JsonProperty("additionalProperties") Map<String, String> additionalProperties,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(selector, resourceId, resourceCrn, secretType, executionType, additionalProperties, accepted);
    }

    public static SecretSubRotationTriggerEvent fromChainTrigger(SecretRotationFlowChainTriggerEvent chainTriggerEvent, SecretType secretType) {
        return new SecretSubRotationTriggerEvent(EventSelectorUtil.selector(SecretSubRotationTriggerEvent.class), chainTriggerEvent.getResourceId(),
                chainTriggerEvent.getResourceCrn(), secretType, chainTriggerEvent.getExecutionType(), chainTriggerEvent.getAdditionalProperties(),
                chainTriggerEvent.accepted());
    }

    @Override
    public String toString() {
        return "SecretSubRotationTriggerEvent{} " + super.toString();
    }
}
