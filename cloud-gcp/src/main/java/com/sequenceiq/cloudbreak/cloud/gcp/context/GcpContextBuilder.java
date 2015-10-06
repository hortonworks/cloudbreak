package com.sequenceiq.cloudbreak.cloud.gcp.context;

import java.util.List;

import org.springframework.stereotype.Service;

import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.template.ResourceContextBuilder;
import com.sequenceiq.cloudbreak.common.type.CloudPlatform;

@Service
public class GcpContextBuilder implements ResourceContextBuilder<GcpContext> {

    public static final int PARALLEL_RESOURCE_REQUEST = 30;

    @Override
    public GcpContext contextInit(CloudContext context, AuthenticatedContext auth, List<CloudResource> resources, boolean build) {
        CloudCredential credential = auth.getCloudCredential();
        String projectId = GcpStackUtil.getProjectId(credential);
        Compute compute = GcpStackUtil.buildCompute(credential);
        return new GcpContext(context.getName(), context.getRegion(), projectId, compute, PARALLEL_RESOURCE_REQUEST, build);
    }

    @Override
    public String platform() {
        return CloudPlatform.GCP.name();
    }
}
