package com.sequenceiq.datalake.flow.create.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxCreateFailedEvent extends SdxEvent {

    private Exception exception;

    public SdxCreateFailedEvent(Long sdxId, String userId, Exception exception) {
        super(sdxId, userId);
        this.exception = exception;
    }

    @Override
    public String selector() {
        return "SdxCreateFailedEvent";
    }

    public Exception getException() {
        return exception;
    }
}
