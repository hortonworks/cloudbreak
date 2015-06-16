package com.sequenceiq.cloudbreak.service.stack.connector.gcp;

import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.domain.CloudRegion;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.StackContext;

public class GcpImageReadyPollerObject extends StackContext {

    private String name;
    private Compute compute;
    private CloudRegion gcpZone;

    public GcpImageReadyPollerObject(Compute compute, Stack stack, String name, CloudRegion gcpZone) {
        super(stack);
        this.compute = compute;
        this.name = name;
        this.gcpZone = gcpZone;
    }

    public CloudRegion getGcpZone() {
        return gcpZone;
    }

    public void setGcpZone(CloudRegion gcpZone) {
        this.gcpZone = gcpZone;
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
