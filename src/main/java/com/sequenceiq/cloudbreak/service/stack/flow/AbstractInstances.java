package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.List;

import com.sequenceiq.cloudbreak.domain.Stack;

public abstract class AbstractInstances {

    private Stack stack;
    private List<String> instances;
    private String status;

    protected AbstractInstances(Stack stack, List<String> instances, String status) {
        this.stack = stack;
        this.instances = instances;
        this.status = status;
    }

    public Stack getStack() {
        return stack;
    }

    public void setStack(Stack stack) {
        this.stack = stack;
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
