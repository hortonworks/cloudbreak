package com.sequenceiq.flow.rotation.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.rotation.chain.SecretRotationFlowChainTriggerEvent;

public class SecretRotationTriggerEvent extends RotationEvent {

    @JsonCreator
    public SecretRotationTriggerEvent(@JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("secretType") SecretType secretType,
            @JsonProperty("executionType") RotationFlowExecutionType executionType,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(selector, resourceId, resourceCrn, secretType, executionType, accepted);
    }

    public static SecretRotationTriggerEvent fromChainTrigger(SecretRotationFlowChainTriggerEvent chainTriggerEvent, SecretType secretType) {
        return new SecretRotationTriggerEvent(EventSelectorUtil.selector(SecretRotationTriggerEvent.class), chainTriggerEvent.getResourceId(),
                chainTriggerEvent.getResourceCrn(), secretType, chainTriggerEvent.getExecutionType(), chainTriggerEvent.accepted());
    }
}
