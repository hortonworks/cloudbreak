package com.sequenceiq.datalake.flow.stop.event;

import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class SdxStopFailedEvent extends SdxFailedEvent {

    public SdxStopFailedEvent(Long sdxId, String userId, Exception exception) {
        super(sdxId, userId, exception);
    }

    @Override
    public String selector() {
        return "SdxStopFailedEvent";
    }
}
