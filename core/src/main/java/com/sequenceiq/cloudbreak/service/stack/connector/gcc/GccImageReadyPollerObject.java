package com.sequenceiq.cloudbreak.service.stack.connector.gcc;

import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.StackDependentPollerObject;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.domain.GccZone;

public class GccImageReadyPollerObject extends StackDependentPollerObject {

    private String name;
    private Compute compute;
    private GccZone gccZone;

    public GccImageReadyPollerObject(Compute compute, Stack stack, String name, GccZone gccZone) {
        super(stack);
        this.compute = compute;
        this.name = name;
        this.gccZone = gccZone;
    }

    public GccZone getGccZone() {
        return gccZone;
    }

    public void setGccZone(GccZone gccZone) {
        this.gccZone = gccZone;
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
