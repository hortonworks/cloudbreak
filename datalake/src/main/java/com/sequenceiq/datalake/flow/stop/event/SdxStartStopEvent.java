package com.sequenceiq.datalake.flow.stop.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxStartStopEvent extends SdxEvent {

    public SdxStartStopEvent(String selector, Long sdxId, String userId) {
        super(selector, sdxId, userId);
    }

    @Override
    public String selector() {
        return "SdxStartStopEvent";
    }
}
