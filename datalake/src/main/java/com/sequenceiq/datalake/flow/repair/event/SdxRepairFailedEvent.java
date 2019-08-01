package com.sequenceiq.datalake.flow.repair.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxRepairFailedEvent extends SdxEvent {

    private Exception exception;

    public SdxRepairFailedEvent(Long sdxId, String userId, String requestId, Exception exception) {
        super(sdxId, userId, requestId);
        this.exception = exception;
    }

    public static SdxRepairFailedEvent from(SdxEvent event, Exception exception) {
        return new SdxRepairFailedEvent(event.getResourceId(), event.getUserId(), event.getRequestId(), exception);
    }

    @Override
    public String selector() {
        return "SdxRepairFailedEvent";
    }

    public Exception getException() {
        return exception;
    }
}
