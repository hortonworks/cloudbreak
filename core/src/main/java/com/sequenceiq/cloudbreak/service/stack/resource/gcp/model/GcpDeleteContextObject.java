package com.sequenceiq.cloudbreak.service.stack.resource.gcp.model;

import java.util.List;

import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.service.stack.resource.DeleteContextObject;

public class GcpDeleteContextObject extends DeleteContextObject {

    private Compute compute;
    private String projectId;

    public GcpDeleteContextObject(Long stackId, String projectId, Compute compute) {
        super(stackId);
        this.projectId = projectId;
        this.compute = compute;
    }

    public GcpDeleteContextObject(Long stackId, String projectId, Compute compute, List<Resource> decommisionResources) {
        super(stackId, decommisionResources);
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
