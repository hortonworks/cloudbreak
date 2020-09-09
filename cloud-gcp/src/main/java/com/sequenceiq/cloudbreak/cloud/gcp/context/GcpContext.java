package com.sequenceiq.cloudbreak.cloud.gcp.context;

import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;

public class GcpContext extends ResourceBuilderContext {

    public static final String PROJECT_ID = "pid";

    private static final String SERVICE_ACCOUNT_ID = "serviceAccountId";

    private static final String COMPUTE = "compute";

    private static final String NO_PUBLIC_IP = "noPublicIp";

    public GcpContext(
        String name,
        Location location,
        String projectId,
        String serviceAccountId,
        Compute compute,
        boolean noPublicIp,
        int parallelResourceRequest,
        boolean build) {
        super(name, location, parallelResourceRequest, build);
        putParameter(PROJECT_ID, projectId);
        putParameter(SERVICE_ACCOUNT_ID, serviceAccountId);
        putParameter(COMPUTE, compute);
        putParameter(NO_PUBLIC_IP, noPublicIp);
    }

    public String getProjectId() {
        return getParameter(PROJECT_ID, String.class);
    }

    public String getServiceAccountId() {
        return getParameter(SERVICE_ACCOUNT_ID, String.class);
    }

    public Compute getCompute() {
        return getParameter(COMPUTE, Compute.class);
    }

    public boolean getNoPublicIp() {
        return getParameter(NO_PUBLIC_IP, Boolean.class);
    }
}
