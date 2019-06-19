package com.sequenceiq.datalake.flow.create.event;

import com.sequenceiq.cloudbreak.common.event.Selectable;

public class EnvWaitRequest implements Selectable {

    private Long sdxId;

    public EnvWaitRequest(Long sdxId) {
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
        return "EnvWaitRequest";
    }
}
