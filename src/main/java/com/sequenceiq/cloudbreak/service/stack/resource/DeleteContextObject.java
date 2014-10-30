package com.sequenceiq.cloudbreak.service.stack.resource;

public abstract class DeleteContextObject {

    private Long stackId;

    protected DeleteContextObject(Long stackId) {
        this.stackId = stackId;
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }


}
