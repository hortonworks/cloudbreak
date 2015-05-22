package com.sequenceiq.cloudbreak.service.stack.connector.gcp;

import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.StackContext;
import com.sequenceiq.cloudbreak.service.stack.connector.gcp.domain.GcpZone;

public class GcpImageReadyPollerObject extends StackContext {

    private String name;
    private Compute compute;
    private GcpZone gcpZone;

    public GcpImageReadyPollerObject(Compute compute, Stack stack, String name, GcpZone gcpZone) {
        super(stack);
        this.compute = compute;
        this.name = name;
        this.gcpZone = gcpZone;
    }

    public GcpZone getGcpZone() {
        return gcpZone;
    }

    public void setGcpZone(GcpZone gcpZone) {
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
