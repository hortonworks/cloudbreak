package com.sequenceiq.cloudbreak.cloud.aws.resource.instance;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.resource.AbstractAwsNativeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;

public abstract class AbstractAwsNativeComputeBuilder extends AbstractAwsNativeResourceBuilder implements ComputeResourceBuilder<AwsContext> {
    @Override
    public List<CloudResourceStatus> checkResources(AwsContext context, AuthenticatedContext auth, List<CloudResource> resources) {
        return checkResources(resourceType(), context, auth, resources);
    }

    @Override
    public CloudVmInstanceStatus start(AwsContext context, AuthenticatedContext auth, CloudInstance instance) {
        return null;
    }

    @Override
    public CloudVmInstanceStatus stop(AwsContext context, AuthenticatedContext auth, CloudInstance instance) {
        return null;
    }

    @Override
    public List<CloudResource> update(AwsContext context, CloudInstance instance, long privateId,
        AuthenticatedContext auth, Group group, CloudStack cloudStack) throws Exception {
        return List.of();
    }

    @Override
    public List<CloudVmInstanceStatus> checkInstances(AwsContext context, AuthenticatedContext auth, List<CloudInstance> instances) {
        return null;
    }
}
