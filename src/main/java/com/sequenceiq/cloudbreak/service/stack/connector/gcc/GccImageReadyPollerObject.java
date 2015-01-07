package com.sequenceiq.cloudbreak.service.stack.connector.gcc;

import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.cluster.flow.StackDependentPollerObject;

public class GccImageReadyPollerObject extends StackDependentPollerObject {

    private String name;
    private Compute compute;

    public GccImageReadyPollerObject(String name, Stack stack, Compute compute) {
        super(stack);
        this.name = name;
        this.compute = compute;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Compute getCompute() {
        return compute;
    }

    public void setCompute(Compute compute) {
        this.compute = compute;
    }
}
