package com.sequenceiq.datalake.flow.repair.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxRepairCouldNotStartEvent extends SdxEvent {

    private Exception exception;

    public SdxRepairCouldNotStartEvent(Long sdxId, String userId, Exception exception) {
        super(sdxId, userId);
        this.exception = exception;
    }

    public static SdxRepairCouldNotStartEvent from(SdxEvent event, Exception exception) {
        return new SdxRepairCouldNotStartEvent(event.getResourceId(), event.getUserId(), exception);
    }

    @Override
    public String selector() {
        return "SdxRepairCouldNotStartEvent";
    }

    public Exception getException() {
        return exception;
    }
}
