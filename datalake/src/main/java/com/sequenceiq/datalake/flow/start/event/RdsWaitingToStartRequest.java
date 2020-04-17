package com.sequenceiq.datalake.flow.start.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class RdsWaitingToStartRequest extends SdxEvent {

    public RdsWaitingToStartRequest(Long sdxId, String userId) {
        super(sdxId, userId);
    }

    @Override
    public String selector() {
        return RdsWaitingToStartRequest.class.getSimpleName();
    }
}
