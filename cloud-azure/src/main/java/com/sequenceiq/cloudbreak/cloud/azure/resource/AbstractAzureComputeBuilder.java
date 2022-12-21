package com.sequenceiq.cloudbreak.cloud.azure.resource;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.azure.context.AzureContext;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;

public abstract class AbstractAzureComputeBuilder extends AbstractAzureResourceBuilder implements ComputeResourceBuilder<AzureContext> {
    @Override
    public List<CloudResourceStatus> checkResources(AzureContext context, AuthenticatedContext auth, List<CloudResource> resources) {
        return checkResources(resourceType(), context, auth, resources);
    }

    @Override
    public List<CloudVmInstanceStatus> checkInstances(AzureContext context, AuthenticatedContext auth, List<CloudInstance> instances) {
        return null;
    }

    @Override
    public CloudVmInstanceStatus stop(AzureContext context, AuthenticatedContext auth, CloudInstance instance) {
        return null;
    }

    @Override
    public CloudVmInstanceStatus start(AzureContext context, AuthenticatedContext auth, CloudInstance instance) {
        return null;
    }

    @Override
    public List<CloudResource> update(AzureContext context, CloudInstance instance, long privateId,
            AuthenticatedContext auth, Group group, CloudStack cloudStack) throws Exception {
        return List.of();
    }
}
