package com.sequenceiq.cloudbreak.cloud.gcp.network;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.AbstractGcpResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.template.NetworkResourceBuilder;
import com.sequenceiq.cloudbreak.common.type.CloudPlatform;

public abstract class AbstractGcpNetworkBuilder extends AbstractGcpResourceBuilder implements NetworkResourceBuilder<GcpContext> {

    @Override
    public List<CloudResourceStatus> checkResources(GcpContext context, AuthenticatedContext auth, List<CloudResource> resources) {
        return checkResources(resourceType(), context, auth, resources);
    }

    @Override
    public CloudResourceStatus update(GcpContext context, AuthenticatedContext auth,
            Network network, Security security, CloudResource resource) throws Exception {
        return null;
    }

    @Override
    public String platform() {
        return CloudPlatform.GCP.name();
    }

}
