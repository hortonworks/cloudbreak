package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.List;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.StackContext;

public abstract class AbstractInstances extends StackContext {

    private List<String> instances;
    private String status;

    protected AbstractInstances(Stack stack, List<String> instances, String status) {
        super(stack);
        this.instances = instances;
        this.status = status;
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
