package com.sequenceiq.datalake.flow.delete.event;

import com.sequenceiq.cloudbreak.common.event.Selectable;

public class StackDeletionWaitRequest implements Selectable {

    private Long sdxId;

    public StackDeletionWaitRequest(Long sdxId) {
        this.sdxId = sdxId;
    }

    @Override
    public Long getResourceId() {
        return sdxId;
    }

    public void setResourceId(Long sdxId) {
        this.sdxId = sdxId;
    }

    @Override
    public String selector() {
        return "StackDeletionWaitRequest";
    }
}
