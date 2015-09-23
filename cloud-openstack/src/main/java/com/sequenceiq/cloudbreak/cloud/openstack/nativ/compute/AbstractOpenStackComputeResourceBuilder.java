package com.sequenceiq.cloudbreak.cloud.openstack.nativ.compute;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.openstack.OpenStackConstants;
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.AbstractOpenStackResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.context.OpenStackContext;
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.service.OpenStackResourceNameService;
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;

public abstract class AbstractOpenStackComputeResourceBuilder extends AbstractOpenStackResourceBuilder implements ComputeResourceBuilder<OpenStackContext> {
    @Inject
    private OpenStackResourceNameService resourceNameService;

    @Override
    public List<CloudResource> create(OpenStackContext context, long privateId, AuthenticatedContext auth, Group group, Image image) {
        CloudContext cloudContext = auth.getCloudContext();
        String resourceName = resourceNameService.resourceName(resourceType(), cloudContext.getStackName(), group.getName(), privateId);
        return Arrays.asList(createNamedResource(resourceType(), resourceName));
    }

    @Override
    public List<CloudVmInstanceStatus> checkInstances(OpenStackContext context, AuthenticatedContext auth, List<CloudInstance> instances) {
        return null;
    }

    @Override
    public CloudResource rollback(OpenStackContext context, CloudResource resource) throws Exception {
        return null;
    }

    @Override
    public CloudVmInstanceStatus start(OpenStackContext context, AuthenticatedContext auth, CloudInstance instance) {
        return null;
    }

    @Override
    public CloudVmInstanceStatus stop(OpenStackContext context, AuthenticatedContext auth, CloudInstance instance) {
        return null;
    }

    @Override
    public List<CloudResourceStatus> checkResources(OpenStackContext context, AuthenticatedContext auth, List<CloudResource> resources) {
        return checkResources(resourceType(), context, auth, resources);
    }

    @Override
    public String platform() {
        return OpenStackConstants.OPENSTACK;
    }

    protected InstanceTemplate getInstanceTemplate(Group group, long privateId) {
        for (InstanceTemplate template : group.getInstances()) {
            if (template.getPrivateId() == privateId) {
                return template;
            }
        }
        return null;
    }
}
