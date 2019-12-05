package com.sequenceiq.datalake.flow.start.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxStartStartEvent extends SdxEvent {

    public SdxStartStartEvent(String selector, Long sdxId, String userId) {
        super(selector, sdxId, userId);
    }

    @Override
    public String selector() {
        return "SdxStartStartEvent";
    }
}
