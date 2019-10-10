package com.sequenceiq.datalake.flow.start.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxStartFailedEvent extends SdxEvent {

    private Exception exception;

    public SdxStartFailedEvent(Long sdxId, String userId, String requestId, Exception exception) {
        super(sdxId, userId, requestId);
        this.exception = exception;
    }

    public static SdxStartFailedEvent from(SdxEvent event, Exception exception) {
        return new SdxStartFailedEvent(event.getResourceId(), event.getUserId(), event.getRequestId(), exception);
    }

    @Override
    public String selector() {
        return "SdxStartFailedEvent";
    }

    public Exception getException() {
        return exception;
    }
}
