package com.sequenceiq.cloudbreak.cloud.gcp.context;

import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.cloud.event.context.ResourceBuilderContext;

public class GcpContext extends ResourceBuilderContext {

    public static final String PROJECT_ID = "pid";
    private static final String COMPUTE = "compute";

    public GcpContext(String name, String region, String projectId, Compute compute, int parallelResourceRequest, boolean build) {
        super(name, region, parallelResourceRequest, build);
        putParameter(PROJECT_ID, projectId);
        putParameter(COMPUTE, compute);
    }

    public String getProjectId() {
        return getParameter(PROJECT_ID, String.class);
    }

    public Compute getCompute() {
        return getParameter(COMPUTE, Compute.class);
    }
}
