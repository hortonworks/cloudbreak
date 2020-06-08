package com.sequenceiq.datalake.flow.create.event;

import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class SdxCreateFailedEvent extends SdxFailedEvent {

    public SdxCreateFailedEvent(Long sdxId, String userId, Exception exception) {
        super(sdxId, userId, exception);
    }

    public static SdxCreateFailedEvent from(SdxEvent event, Exception exception) {
        return new SdxCreateFailedEvent(event.getResourceId(), event.getUserId(), exception);
    }

    @Override
    public String selector() {
        return "SdxCreateFailedEvent";
    }
}
