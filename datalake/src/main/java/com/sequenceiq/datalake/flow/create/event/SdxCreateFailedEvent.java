package com.sequenceiq.datalake.flow.create.event;

import com.sequenceiq.cloudbreak.common.event.Selectable;

public class SdxCreateFailedEvent implements Selectable {

    private Long sdxId;

    private Exception exception;

    public SdxCreateFailedEvent(Long sdxId, Exception exception) {
        this.sdxId = sdxId;
        this.exception = exception;
    }

    @Override
    public String selector() {
        return "SdxCreateFailedEvent";
    }

    @Override
    public Long getResourceId() {
        return sdxId;
    }

    public Exception getException() {
        return exception;
    }
}
