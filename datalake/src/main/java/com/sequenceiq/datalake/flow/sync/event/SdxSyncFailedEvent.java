package com.sequenceiq.datalake.flow.sync.event;

import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class SdxSyncFailedEvent extends SdxFailedEvent {

    public SdxSyncFailedEvent(Long sdxId, String userId, Exception exception) {
        super(sdxId, userId, exception);
    }

    @Override
    public String selector() {
        return "SdxSyncFailedEvent";
    }
}
