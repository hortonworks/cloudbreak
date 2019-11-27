package com.sequenceiq.datalake.flow.stop.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxStopFailedEvent extends SdxEvent {

    private Exception exception;

    public SdxStopFailedEvent(Long sdxId, String userId, Exception exception) {
        super(sdxId, userId);
        this.exception = exception;
    }

    public static SdxStopFailedEvent from(SdxEvent event, Exception exception) {
        return new SdxStopFailedEvent(event.getResourceId(), event.getUserId(), exception);
    }

    @Override
    public String selector() {
        return "SdxStopFailedEvent";
    }

    public Exception getException() {
        return exception;
    }
}
