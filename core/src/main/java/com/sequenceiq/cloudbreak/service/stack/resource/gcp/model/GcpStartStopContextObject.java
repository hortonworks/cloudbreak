package com.sequenceiq.cloudbreak.service.stack.resource.gcp.model;

import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.resource.StartStopContextObject;

public class GcpStartStopContextObject extends StartStopContextObject {

    private Compute compute;

    public GcpStartStopContextObject(Stack stack, Compute compute) {
        super(stack);
        this.compute = compute;
    }

    public Compute getCompute() {
        return compute;
    }
}
