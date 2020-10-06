package com.sequenceiq.datalake.flow.cert.rotation.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxCertRotationFailedEvent extends SdxEvent {

    private final Exception exception;

    public SdxCertRotationFailedEvent(Long sdxId, String userId, Exception exception) {
        super(sdxId, userId);
        this.exception = exception;
    }

    public SdxCertRotationFailedEvent(SdxEvent event, Exception exception) {
        super(event.getResourceId(), event.getUserId());
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }
}
