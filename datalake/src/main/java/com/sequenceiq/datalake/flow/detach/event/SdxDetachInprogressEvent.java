package com.sequenceiq.datalake.flow.detach.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxDetachInprogressEvent extends SdxEvent {

    public SdxDetachInprogressEvent(String selector, Long sdxId, String userId) {
        super(selector, sdxId, userId);
    }

    @Override
    public String selector() {
        return "SdxDetachInprogressEvent";
    }
}
