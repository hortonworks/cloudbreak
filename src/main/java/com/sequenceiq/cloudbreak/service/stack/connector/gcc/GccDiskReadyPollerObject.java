package com.sequenceiq.cloudbreak.service.stack.connector.gcc;

import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.domain.Stack;

public class GccDiskReadyPollerObject {

    private Compute compute;
    private Stack stack;
    private String name;

    public GccDiskReadyPollerObject(Compute compute, Stack stack, String name) {
        this.compute = compute;
        this.stack = stack;
        this.name = name;
    }

    public Compute getCompute() {
        return compute;
    }

    public void setCompute(Compute compute) {
        this.compute = compute;
    }

    public Stack getStack() {
        return stack;
    }

    public void setStack(Stack stack) {
        this.stack = stack;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
