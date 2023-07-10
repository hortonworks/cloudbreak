package com.sequenceiq.cloudbreak.rotation.flow.status.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.rotation.flow.chain.SecretRotationFlowChainTriggerEvent;
import com.sequenceiq.cloudbreak.rotation.flow.status.config.SecretRotationStatusChangeEvent;

public class RotationStatusChangeTriggerEvent extends RotationStatusChangeEvent {

    @JsonCreator
    public RotationStatusChangeTriggerEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("start") boolean start,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(SecretRotationStatusChangeEvent.SECRET_ROTATION_STATUS_CHANGE_TRIGGER_EVENT.event(), resourceId, resourceCrn, start, accepted);
    }

    public static RotationStatusChangeTriggerEvent fromChainTrigger(SecretRotationFlowChainTriggerEvent chainTriggerEvent, boolean start) {
        return new RotationStatusChangeTriggerEvent(chainTriggerEvent.getResourceId(), chainTriggerEvent.getResourceCrn(),
                start, chainTriggerEvent.accepted());
    }

}
