package com.sequenceiq.datalake.flow.stop.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class RdsStopSuccessEvent extends SdxEvent {

    public RdsStopSuccessEvent(Long sdxId, String userId) {
        super(sdxId, userId);
    }

    @Override
    public String selector() {
        return RdsStopSuccessEvent.class.getSimpleName();
    }
}
