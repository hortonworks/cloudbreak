package com.sequenceiq.datalake.flow.start.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxStartSuccessEvent extends SdxEvent {

    public SdxStartSuccessEvent(Long sdxId, String userId) {
        super(sdxId, userId);
    }

    @Override
    public String selector() {
        return "SdxStartSuccessEvent";
    }
}
