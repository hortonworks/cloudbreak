package com.sequenceiq.datalake.flow.start.event;

import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class SdxStartFailedEvent extends SdxFailedEvent {

    public SdxStartFailedEvent(Long sdxId, String userId, Exception exception) {
        super(sdxId, userId, exception);
    }

    @Override
    public String selector() {
        return "SdxStartFailedEvent";
    }
}
