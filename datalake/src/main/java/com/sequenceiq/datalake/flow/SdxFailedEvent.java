package com.sequenceiq.datalake.flow;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.CLASS;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

public abstract class SdxFailedEvent extends SdxEvent {

    @JsonTypeInfo(use = CLASS, property = "@type")
    private final Exception exception;

    public SdxFailedEvent(Long sdxId, String userId, Exception exception) {
        super(sdxId, userId);
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }
}
