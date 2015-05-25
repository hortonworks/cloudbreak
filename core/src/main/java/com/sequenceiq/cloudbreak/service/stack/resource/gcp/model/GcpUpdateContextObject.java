package com.sequenceiq.cloudbreak.service.stack.resource.gcp.model;

import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.resource.UpdateContextObject;

public class GcpUpdateContextObject extends UpdateContextObject {

    private Compute compute;
    private String project;

    public GcpUpdateContextObject(Stack stack, Compute compute, String project) {
        super(stack);
        this.compute = compute;
        this.project = project;
    }

    public Compute getCompute() {
        return compute;
    }

    public String getProject() {
        return project;
    }
}
