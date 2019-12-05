package com.sequenceiq.datalake.flow.upgrade.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxImageChangedEvent extends SdxEvent {

    public SdxImageChangedEvent(Long sdxId, String userId) {
        super(sdxId, userId);
    }

    @Override
    public String selector() {
        return "SdxImageChangedEvent";
    }
}
