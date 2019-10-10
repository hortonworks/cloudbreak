package com.sequenceiq.datalake.flow.start.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxStartStartEvent extends SdxEvent {

    public SdxStartStartEvent(String selector, Long sdxId, String userId, String requestId) {
        super(selector, sdxId, userId, requestId);
    }

    @Override
    public String selector() {
        return "SdxStartStartEvent";
    }
}
