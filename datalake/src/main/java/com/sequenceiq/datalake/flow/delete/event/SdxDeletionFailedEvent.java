package com.sequenceiq.datalake.flow.delete.event;

import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class SdxDeletionFailedEvent extends SdxFailedEvent {

    public SdxDeletionFailedEvent(Long sdxId, String userId, Exception exception) {
        super(sdxId, userId, exception);
    }

    public static SdxDeletionFailedEvent from(SdxEvent event, Exception exception) {
        return new SdxDeletionFailedEvent(event.getResourceId(), event.getUserId(), exception);
    }

    @Override
    public String selector() {
        return "SdxDeletionFailedEvent";
    }
}
