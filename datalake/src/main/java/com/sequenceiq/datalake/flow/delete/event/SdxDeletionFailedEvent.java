package com.sequenceiq.datalake.flow.delete.event;

import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class SdxDeletionFailedEvent extends SdxFailedEvent {

    public SdxDeletionFailedEvent(Long sdxId, String userId, Exception exception) {
        super(sdxId, userId, exception);
    }

    @Override
    public String selector() {
        return "SdxDeletionFailedEvent";
    }
}
