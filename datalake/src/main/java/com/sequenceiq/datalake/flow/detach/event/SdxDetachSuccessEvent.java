package com.sequenceiq.datalake.flow.detach.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxDetachSuccessEvent extends SdxEvent {

    public SdxDetachSuccessEvent(String selector, Long sdxId, String userId) {
        super(selector, sdxId, userId);
    }

    @Override
    public String selector() {
        return "SdxDetachSuccessEvent";
    }
}
