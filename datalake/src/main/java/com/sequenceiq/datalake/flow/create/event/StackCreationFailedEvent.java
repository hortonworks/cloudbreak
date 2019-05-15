package com.sequenceiq.datalake.flow.create.event;

import com.sequenceiq.cloudbreak.common.event.Selectable;

public class StackCreationFailedEvent implements Selectable {

    private Long sdxId;

    private Exception exception;

    public StackCreationFailedEvent(Long sdxId, Exception exception) {
        this.sdxId = sdxId;
        this.exception = exception;
    }

    @Override
    public String selector() {
        return "StackCreationFailedEvent";
    }

    @Override
    public Long getResourceId() {
        return sdxId;
    }

    public Exception getException() {
        return exception;
    }
}
