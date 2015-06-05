package com.sequenceiq.cloudbreak.cloud;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Instance;
import com.sequenceiq.cloudbreak.cloud.notification.ResourcePersistenceNotifier;

public interface ResourceConnector {

    // Resources

    List<CloudResourceStatus> launch(AuthenticatedContext authenticatedContext, CloudStack stack, ResourcePersistenceNotifier notifier);

    List<CloudResourceStatus> check(AuthenticatedContext authenticatedContext, List<CloudResource> resources);

    List<CloudResourceStatus> terminate(AuthenticatedContext authenticatedContext, List<CloudResource> cloudResources);

    //e.g. security rules
    List<CloudResourceStatus> update(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources);

    List<CloudResourceStatus> upscale(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources, int adjustment);

    List<CloudResourceStatus> downscale(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources, List<Instance> vms);

}
