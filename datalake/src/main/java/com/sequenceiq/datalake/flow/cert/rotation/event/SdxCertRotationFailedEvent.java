package com.sequenceiq.datalake.flow.cert.rotation.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxCertRotationFailedEvent extends SdxEvent {

    private final Exception exception;

    @JsonCreator
    public SdxCertRotationFailedEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("exception") Exception exception) {
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
