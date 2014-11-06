package com.sequenceiq.cloudbreak.service.stack.resource;

public abstract class StartStopContextObject {

    private Long stackId;

    protected StartStopContextObject(Long stackId) {
        this.stackId = stackId;
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }
}
