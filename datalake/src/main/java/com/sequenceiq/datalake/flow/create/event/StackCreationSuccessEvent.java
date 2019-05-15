package com.sequenceiq.datalake.flow.create.event;

import com.sequenceiq.cloudbreak.common.event.Selectable;

public class StackCreationSuccessEvent implements Selectable {

    private Long sdxId;

    public StackCreationSuccessEvent(Long sdxId) {
        this.sdxId = sdxId;
    }

    @Override
    public String selector() {
        return "StackCreationSuccessEvent";
    }

    @Override
    public Long getResourceId() {
        return sdxId;
    }
}
