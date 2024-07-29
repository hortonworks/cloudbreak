package com.sequenceiq.datalake.flow.dr.validation.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class DatalakeRestoreValidationFailedEvent extends SdxFailedEvent {
    @JsonCreator
    public DatalakeRestoreValidationFailedEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("exception") Exception exception) {
        super(sdxId, userId, exception);
    }

    public static DatalakeRestoreValidationFailedEvent from(SdxEvent event, Exception exception) {
        return new DatalakeRestoreValidationFailedEvent(event.getResourceId(), event.getUserId(), exception);
    }

    @Override
    public String toString() {
        return "DatalakeRestoreValidationFailedEvent{} " + super.toString();
    }
}
