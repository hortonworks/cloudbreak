package com.sequenceiq.cloudbreak.service.stack.resource.gcp.model;

import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.service.stack.resource.ProvisionContextObject;

public class GcpProvisionContextObject extends ProvisionContextObject {

    private String projectId;
    private Compute compute;

    public GcpProvisionContextObject(Long stackId, String projectId, Compute compute) {
        super(stackId);
        this.projectId = projectId;
        this.compute = compute;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public Compute getCompute() {
        return compute;
    }

    public void setCompute(Compute compute) {
        this.compute = compute;
    }

}
