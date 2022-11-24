package com.sequenceiq.cloudbreak.cloud.gcp.compute;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.AbstractGcpResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;

public abstract class AbstractGcpComputeBuilder extends AbstractGcpResourceBuilder implements ComputeResourceBuilder<GcpContext> {
    @Override
    public List<CloudResourceStatus> checkResources(GcpContext context, AuthenticatedContext auth, List<CloudResource> resources) {
        return checkResources(resourceType(), context, auth, resources);
    }

    @Override
    public List<CloudVmInstanceStatus> checkInstances(GcpContext context, AuthenticatedContext auth, List<CloudInstance> instances) {
        return null;
    }

    @Override
    public CloudVmInstanceStatus stop(GcpContext context, AuthenticatedContext auth, CloudInstance instance) {
        return null;
    }

    @Override
    public CloudVmInstanceStatus start(GcpContext context, AuthenticatedContext auth, CloudInstance instance) {
        return null;
    }

    @Override
    public CloudResource update(GcpContext context, CloudResource cloudResource, CloudInstance instance,
            AuthenticatedContext auth, CloudStack cloudStack) throws Exception {
        return null;
    }
}
