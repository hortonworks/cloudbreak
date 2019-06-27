package com.sequenceiq.datalake.flow.delete.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class StackDeletionFailedEvent extends SdxEvent {

    private Exception exception;

    public StackDeletionFailedEvent(Long sdxId, String userId, Exception exception) {
        super(sdxId, userId);
        this.exception = exception;
    }

    @Override
    public String selector() {
        return "StackDeletionFailedEvent";
    }

    public Exception getException() {
        return exception;
    }
}
