package com.sequenceiq.cloudbreak.service.stack.resource;

public abstract class ProvisionContextObject {

    private Long stackId;

    protected ProvisionContextObject(Long stackId) {
        this.stackId = stackId;
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }


}
