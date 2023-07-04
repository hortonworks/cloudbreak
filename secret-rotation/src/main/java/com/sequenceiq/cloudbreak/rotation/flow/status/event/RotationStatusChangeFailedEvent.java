package com.sequenceiq.cloudbreak.rotation.flow.status.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.rotation.flow.status.SecretRotationStatusChangeEvent;

public class RotationStatusChangeFailedEvent extends RotationStatusChangeEvent {

    private final Exception exception;

    @JsonCreator
    public RotationStatusChangeFailedEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("start") boolean start,
            @JsonProperty("exception") Exception exception) {
        super(SecretRotationStatusChangeEvent.SECRET_ROTATION_STATUS_CHANGE_FAILED_EVENT.event(), resourceId, resourceCrn, start);
        this.exception = exception;
    }

    public static RotationStatusChangeFailedEvent fromPayload(RotationStatusChangeEvent payload, Exception exception) {
        return new RotationStatusChangeFailedEvent(payload.getResourceId(), payload.getResourceCrn(), payload.isStart(), exception);
    }

    public Exception getException() {
        return exception;
    }
}
