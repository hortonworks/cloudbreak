package com.sequenceiq.cloudbreak.service.stack.resource.gcc.model;

import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.service.stack.resource.DescribeContextObject;

public class GccDescribeContextObject extends DescribeContextObject {

    private Compute compute;
    private String projectId;

    public GccDescribeContextObject(Long stackId, String projectId, Compute compute) {
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
