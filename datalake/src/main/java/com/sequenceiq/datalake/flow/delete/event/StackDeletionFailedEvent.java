package com.sequenceiq.datalake.flow.delete.event;

import com.sequenceiq.cloudbreak.common.event.Selectable;

public class StackDeletionFailedEvent implements Selectable {

    private Long sdxId;

    private Exception exception;

    public StackDeletionFailedEvent(Long sdxId, Exception exception) {
        this.sdxId = sdxId;
        this.exception = exception;
    }

    @Override
    public String selector() {
        return "StackDeletionFailedEvent";
    }

    @Override
    public Long getResourceId() {
        return sdxId;
    }

    public Exception getException() {
        return exception;
    }
}
