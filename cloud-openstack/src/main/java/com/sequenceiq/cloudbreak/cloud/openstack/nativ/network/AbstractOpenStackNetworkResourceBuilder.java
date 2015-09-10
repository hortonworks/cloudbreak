package com.sequenceiq.cloudbreak.cloud.openstack.nativ.network;

import java.util.List;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.openstack.OpenStackClient;
import com.sequenceiq.cloudbreak.cloud.openstack.OpenStackConstants;
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.AbstractOpenStackResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.context.OpenStackContext;
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.service.OpenStackResourceNameService;
import com.sequenceiq.cloudbreak.cloud.template.NetworkResourceBuilder;

public abstract class AbstractOpenStackNetworkResourceBuilder extends AbstractOpenStackResourceBuilder implements NetworkResourceBuilder<OpenStackContext> {
    @Inject
    private OpenStackResourceNameService resourceNameService;
    @Inject
    private OpenStackClient openStackClient;

    @Override
    public CloudResource create(OpenStackContext context, AuthenticatedContext auth, Network network, Security security) {
        String resourceName = resourceNameService.resourceName(resourceType(), context.getName());
        return createNamedResource(resourceType(), resourceName);
    }

    @Override
    public CloudResourceStatus update(OpenStackContext context, AuthenticatedContext auth, Network network, Security security, CloudResource resource)
            throws Exception {
        return null;
    }

    @Override
    public String platform() {
        return OpenStackConstants.OPENSTACK_NATIVE;
    }

    @Override
    public List<CloudResourceStatus> checkResources(OpenStackContext context, AuthenticatedContext auth, List<CloudResource> resources) {
        return checkResources(resourceType(), context, auth, resources);
    }
}
