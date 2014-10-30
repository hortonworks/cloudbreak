package com.sequenceiq.cloudbreak.service.stack.resource;

public abstract class DescribeContextObject {

    private Long stackId;

    protected DescribeContextObject(Long stackId) {
        this.stackId = stackId;
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }


}
