package com.sequenceiq.datalake.flow.datalake.recovery.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class DatalakeRecoveryFailedEvent extends SdxFailedEvent {

    @JsonCreator
    public DatalakeRecoveryFailedEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("exception") Exception exception) {
        super(sdxId, userId, exception);
    }

    public static DatalakeRecoveryFailedEvent from(SdxEvent event, Exception exception) {
        return new DatalakeRecoveryFailedEvent(event.getResourceId(), event.getUserId(), exception);
    }

    @Override
    public String toString() {
        return "DatalakeRecoveryFailedEvent{} " + super.toString();
    }
}
