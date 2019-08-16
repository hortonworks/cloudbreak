package com.sequenceiq.datalake.flow.delete.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxDeletionFailedEvent extends SdxEvent {

    private Exception exception;

    public SdxDeletionFailedEvent(Long sdxId, String userId, String requestId, Exception exception) {
        super(sdxId, userId, requestId);
        this.exception = exception;
    }

    public static SdxDeletionFailedEvent from(SdxEvent event, Exception exception) {
        return new SdxDeletionFailedEvent(event.getResourceId(), event.getUserId(), event.getRequestId(), exception);
    }

    @Override
    public String selector() {
        return "SdxDeletionFailedEvent";
    }

    public Exception getException() {
        return exception;
    }
}
