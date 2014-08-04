package com.sequenceiq.cloudbreak.service.stack.event;

public class StackDeleteComplete {

    private Long stackId;

    public StackDeleteComplete(Long stackId) {
        this.stackId = stackId;
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

}
