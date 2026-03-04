package com.sequenceiq.cloudbreak.cloud.openstack.securitygroup;

import java.util.List;

import jakarta.inject.Inject;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.openstack.AbstractOpenStackResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackResourceNameService;
import com.sequenceiq.cloudbreak.cloud.openstack.context.OpenStackContext;
import com.sequenceiq.cloudbreak.cloud.template.GroupResourceBuilder;

public abstract class AbstractOpenStackGroupResourceBuilder extends AbstractOpenStackResourceBuilder implements GroupResourceBuilder<OpenStackContext> {

    @Inject
    private OpenStackResourceNameService resourceNameService;

    @Override
    public List<CloudResourceStatus> checkResources(OpenStackContext context, AuthenticatedContext auth, List<CloudResource> resources) {
        return checkResources(resourceType(), context, auth, resources);
    }

    @Override
    public CloudResource create(OpenStackContext context, AuthenticatedContext auth, Group group, Network network) {
        String resourceName = resourceNameService.resourceName(resourceType(), context.getName(), group.getName(), auth.getCloudContext().getId());
        return createNamedResource(resourceType(), group.getName(), resourceName);
    }

    @Override
    public CloudResourceStatus update(OpenStackContext context, AuthenticatedContext auth, Group group, Network network, Security security,
            CloudResource resource) {
        return null;
    }
}
