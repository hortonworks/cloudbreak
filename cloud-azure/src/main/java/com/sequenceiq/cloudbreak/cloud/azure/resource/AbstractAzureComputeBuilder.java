package com.sequenceiq.cloudbreak.cloud.azure.resource;

import java.util.List;
import java.util.Optional;

import com.sequenceiq.cloudbreak.cloud.azure.context.AzureContext;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
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
    public CloudResource update(AzureContext context, CloudResource cloudResource, CloudInstance instance,
            AuthenticatedContext auth, CloudStack cloudStack, Optional<String> targetGroupName) throws Exception {
        return null;
    }
}
