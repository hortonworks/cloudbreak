package com.sequenceiq.datalake.flow.datalake.recovery.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeRecoveryWaitRequest extends SdxEvent {

    @JsonCreator
    public DatalakeRecoveryWaitRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }

    public static DatalakeRecoveryWaitRequest from(SdxContext context) {
        return new DatalakeRecoveryWaitRequest(context.getSdxId(), context.getUserId());
    }

    @Override
    public String toString() {
        return "DatalakeRecoveryWaitRequest{} " + super.toString();
    }
}
