package com.sequenceiq.datalake.flow.stop.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxStopSuccessEvent extends SdxEvent {

    public SdxStopSuccessEvent(Long sdxId, String userId) {
        super(sdxId, userId);
    }

    @Override
    public String selector() {
        return "SdxStopSuccessEvent";
    }
}
