package com.sequenceiq.cloudbreak.service.stack.resource.gcc.model;

import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.resource.StartStopContextObject;

public class GccStartStopContextObject extends StartStopContextObject {

    private Compute compute;

    public GccStartStopContextObject(Stack stack, Compute compute) {
        super(stack);
        this.compute = compute;
    }

    public Compute getCompute() {
        return compute;
    }
}
