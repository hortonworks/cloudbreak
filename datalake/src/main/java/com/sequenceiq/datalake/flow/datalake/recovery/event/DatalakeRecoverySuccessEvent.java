package com.sequenceiq.datalake.flow.datalake.recovery.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeRecoverySuccessEvent extends SdxEvent {

    @JsonCreator
    public DatalakeRecoverySuccessEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }

    @Override
    public String toString() {
        return "DatalakeRecoverySuccessEvent{} " + super.toString();
    }
}
