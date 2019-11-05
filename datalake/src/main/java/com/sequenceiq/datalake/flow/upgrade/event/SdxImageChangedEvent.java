package com.sequenceiq.datalake.flow.upgrade.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxImageChangedEvent extends SdxEvent {

    public SdxImageChangedEvent(Long sdxId, String userId, String requestId) {
        super(sdxId, userId, requestId);
    }

    @Override
    public String selector() {
        return "SdxImageChangedEvent";
    }
}
