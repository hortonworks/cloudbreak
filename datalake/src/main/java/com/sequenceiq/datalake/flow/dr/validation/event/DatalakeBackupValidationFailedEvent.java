package com.sequenceiq.datalake.flow.dr.validation.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class DatalakeBackupValidationFailedEvent extends SdxFailedEvent {
    @JsonCreator
    public DatalakeBackupValidationFailedEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("exception") Exception exception) {
        super(sdxId, userId, exception);
    }

    public static DatalakeBackupValidationFailedEvent from(SdxEvent event, Exception exception) {
        return new DatalakeBackupValidationFailedEvent(event.getResourceId(), event.getUserId(), exception);
    }

    @Override
    public String toString() {
        return "DatalakeBackupFailedEvent{" +
                "exception= " + getException().toString() +
                '}';
    }
}
