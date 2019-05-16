package com.sequenceiq.datalake.flow.delete.event;

import com.sequenceiq.cloudbreak.common.event.Selectable;

public class StackDeletionSuccessEvent implements Selectable {

    private Long sdxId;

    public StackDeletionSuccessEvent(Long sdxId) {
        this.sdxId = sdxId;
    }

    @Override
    public String selector() {
        return "StackDeletionSuccessEvent";
    }

    @Override
    public Long getResourceId() {
        return sdxId;
    }
}
