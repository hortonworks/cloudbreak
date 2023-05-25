package com.sequenceiq.flow.rotation.status.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.flow.rotation.status.SecretRotationStatusChangeEvent;

public class RotationStatusChangeFailHandledEvent extends RotationStatusChangeEvent {

    @JsonCreator
    public RotationStatusChangeFailHandledEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("start") boolean start) {
        super(SecretRotationStatusChangeEvent.SECRET_ROTATION_STATUS_CHANGE_FAIL_HANDLED_EVENT.event(), resourceId, resourceCrn, start);
    }

    public static RotationStatusChangeFailHandledEvent fromPayload(RotationStatusChangeEvent payload) {
        return new RotationStatusChangeFailHandledEvent(payload.getResourceId(), payload.getResourceCrn(), payload.isStart());
    }
}
