package com.sequenceiq.datalake.flow.stop.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class RdsWaitingToStopRequest extends SdxEvent {

    public RdsWaitingToStopRequest(Long sdxId, String userId) {
        super(sdxId, userId);
    }

    @Override
    public String selector() {
        return RdsWaitingToStopRequest.class.getSimpleName();
    }
}
