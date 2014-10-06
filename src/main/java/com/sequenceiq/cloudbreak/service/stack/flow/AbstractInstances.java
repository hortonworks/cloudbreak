package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.List;

public abstract class AbstractInstances {

    private long stackId;
    private List<String> instances;
    private String status;

    protected AbstractInstances(long stackId, List<String> instances, String status) {
        this.stackId = stackId;
        this.instances = instances;
        this.status = status;
    }

    public long getStackId() {
        return stackId;
    }

    public void setStackId(long stackId) {
        this.stackId = stackId;
    }

    public List<String> getInstances() {
        return instances;
    }

    public void setInstances(List<String> instances) {
        this.instances = instances;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
