package com.sequenceiq.datalake.flow.start.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxStartFailedEvent extends SdxEvent {

    private Exception exception;

    public SdxStartFailedEvent(Long sdxId, String userId, Exception exception) {
        super(sdxId, userId);
        this.exception = exception;
    }

    public static SdxStartFailedEvent from(SdxEvent event, Exception exception) {
        return new SdxStartFailedEvent(event.getResourceId(), event.getUserId(), exception);
    }

    @Override
    public String selector() {
        return "SdxStartFailedEvent";
    }

    public Exception getException() {
        return exception;
    }
}
