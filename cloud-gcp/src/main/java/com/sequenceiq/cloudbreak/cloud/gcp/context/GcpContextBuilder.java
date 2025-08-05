package com.sequenceiq.cloudbreak.cloud.gcp.context;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpConstants;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpComputeFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.template.ResourceContextBuilder;

@Service
public class GcpContextBuilder implements ResourceContextBuilder<GcpContext> {

    @Inject
    private GcpComputeFactory gcpComputeFactory;

    @Inject
    private GcpStackUtil gcpStackUtil;

    @Value("${gcp.resource.builder.pool.size:20}")
    private int resourceBuilderPoolSize;

    @Override
    public GcpContext contextInit(CloudContext context, AuthenticatedContext auth, Network network, boolean build) {
        CloudCredential credential = auth.getCloudCredential();
        String projectId = gcpStackUtil.getProjectId(credential);
        String serviceAccountId = gcpStackUtil.getServiceAccountId(credential);
        Compute compute = gcpComputeFactory.buildCompute(credential);
        Location location = context.getLocation();
        boolean noPublicIp = network != null ? gcpStackUtil.noPublicIp(network) : false;
        return new GcpContext(context.getName(), location, projectId, serviceAccountId, compute, noPublicIp, resourceBuilderPoolSize, build);
    }

    @Override
    public Platform platform() {
        return GcpConstants.GCP_PLATFORM;
    }

    @Override
    public Variant variant() {
        return GcpConstants.GCP_VARIANT;
    }
}
