package com.sequenceiq.flow.rotation.status.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.flow.rotation.status.SecretRotationStatusChangeEvent;

public class RotationStatusChangeFinishedEvent extends RotationStatusChangeEvent {

    @JsonCreator
    public RotationStatusChangeFinishedEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("start") boolean start) {
        super(SecretRotationStatusChangeEvent.SECRET_ROTATION_STATUS_CHANGE_FINISHED_EVENT.event(), resourceId, resourceCrn, start);
    }

    public static RotationStatusChangeFinishedEvent fromPayload(RotationStatusChangeEvent payload) {
        return new RotationStatusChangeFinishedEvent(payload.getResourceId(), payload.getResourceCrn(), payload.isStart());
    }
}
