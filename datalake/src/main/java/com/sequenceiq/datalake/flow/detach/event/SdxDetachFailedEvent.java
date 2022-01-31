package com.sequenceiq.datalake.flow.detach.event;

import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class SdxDetachFailedEvent extends SdxFailedEvent {

    public SdxDetachFailedEvent(Long sdxId, String userId,  Exception exception) {
        super(sdxId, userId, exception);
    }

    public static SdxDetachFailedEvent from(SdxEvent event, Exception exception) {
        return new SdxDetachFailedEvent(event.getResourceId(), event.getUserId(), exception);
    }

    @Override
    public String selector() {
        return "SdxDetachFailedEvent";
    }
}
