package com.sequenceiq.datalake.flow.start.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxSyncSuccessEvent extends SdxEvent {

    public SdxSyncSuccessEvent(Long sdxId, String userId) {
        super(sdxId, userId);
    }

    @Override
    public String selector() {
        return "SdxSyncSuccessEvent";
    }
}
