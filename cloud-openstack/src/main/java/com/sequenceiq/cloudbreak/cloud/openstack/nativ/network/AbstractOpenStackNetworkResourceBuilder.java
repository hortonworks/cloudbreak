package com.sequenceiq.cloudbreak.cloud.openstack.nativ.network;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.AbstractOpenStackResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.OpenStackResourceException;
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.context.OpenStackContext;
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.service.OpenStackResourceNameService;
import com.sequenceiq.cloudbreak.cloud.template.NetworkResourceBuilder;

public abstract class AbstractOpenStackNetworkResourceBuilder extends AbstractOpenStackResourceBuilder implements NetworkResourceBuilder<OpenStackContext> {

    private static final Map<Class<? extends AbstractOpenStackNetworkResourceBuilder>, Integer> ORDER;

    static {
        ORDER = Map.of(OpenStackNetworkResourceBuilder.class, 0, OpenStackSubnetResourceBuilder.class, 1, OpenStackRouterResourceBuilder.class, 2);
    }

    @Inject
    private OpenStackResourceNameService resourceNameService;

    @Override
    public CloudResource create(OpenStackContext context, AuthenticatedContext auth, Network network) {
        String resourceName = resourceNameService.resourceName(resourceType(), context.getName());
        return createNamedResource(resourceType(), null, resourceName);
    }

    @Override
    public CloudResourceStatus update(OpenStackContext context, AuthenticatedContext auth, Network network, Security security, CloudResource resource) {
        return null;
    }

    @Override
    public int order() {
        Integer order = ORDER.get(getClass());
        if (order == null) {
            throw new OpenStackResourceException(String.format("No resource order found for class: %s", getClass()));
        }
        return order;
    }

    @Override
    public List<CloudResourceStatus> checkResources(OpenStackContext context, AuthenticatedContext auth, List<CloudResource> resources) {
        return checkResources(resourceType(), context, auth, resources);
    }
}
