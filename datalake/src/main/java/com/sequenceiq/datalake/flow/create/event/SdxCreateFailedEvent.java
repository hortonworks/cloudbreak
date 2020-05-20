package com.sequenceiq.datalake.flow.create.event;

import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class SdxCreateFailedEvent extends SdxFailedEvent {

    public SdxCreateFailedEvent(Long sdxId, String userId, Exception exception) {
        super(sdxId, userId, exception);
    }

    @Override
    public String selector() {
        return "SdxCreateFailedEvent";
    }
}
