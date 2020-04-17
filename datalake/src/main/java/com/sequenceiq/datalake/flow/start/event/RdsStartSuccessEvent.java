package com.sequenceiq.datalake.flow.start.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class RdsStartSuccessEvent extends SdxEvent {

    public RdsStartSuccessEvent(Long sdxId, String userId) {
        super(sdxId, userId);
    }

    @Override
    public String selector() {
        return RdsStartSuccessEvent.class.getSimpleName();
    }
}
