package com.sequenceiq.cloudbreak.cloud.gcp.network;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.common.api.type.ResourceType;

@Component
class TestGcpNetworkBuilder extends AbstractGcpNetworkBuilder {

    @Override
    public CloudResource create(GcpContext context, AuthenticatedContext auth, Network network) {
        return null;
    }

    @Override
    public CloudResource build(GcpContext context, AuthenticatedContext auth, Network network, Security security, CloudResource resource) throws Exception {
        return null;
    }

    @Override
    public CloudResource delete(GcpContext context, AuthenticatedContext auth, CloudResource resource, Network network) throws Exception {
        return null;
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.GCP_NETWORK;
    }

    @Override
    public int order() {
        return 0;
    }
}
