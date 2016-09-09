package com.sequenceiq.cloudbreak.cloud.task;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.InstanceConsoleOutputResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.InstancesStatusResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

@Component
public class PollTaskFactory {
    @Inject
    private ApplicationContext applicationContext;
    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    public PollTask<ResourcesStatePollerResult> newPollResourcesStateTask(AuthenticatedContext authenticatedContext,
            List<CloudResource> cloudResource, boolean cancellable) {
        CloudConnector connector = cloudPlatformConnectors.get(authenticatedContext.getCloudContext().getPlatformVariant());
        return createPollTask(PollResourcesStateTask.NAME, authenticatedContext, connector.resources(), cloudResource, cancellable);
    }

    public PollTask<InstancesStatusResult> newPollInstanceStateTask(AuthenticatedContext authenticatedContext, List<CloudInstance> instances,
            Set<InstanceStatus> completedStatuses) {
        CloudConnector connector = cloudPlatformConnectors.get(authenticatedContext.getCloudContext().getPlatformVariant());
        return createPollTask(PollInstancesStateTask.NAME, authenticatedContext, connector.instances(), instances, completedStatuses);
    }

    public PollTask<InstanceConsoleOutputResult> newPollConsoleOutputTask(InstanceConnector instanceConnector,
            AuthenticatedContext authenticatedContext, CloudInstance instance) {
        return createPollTask(PollInstanceConsoleOutputTask.NAME, instanceConnector, authenticatedContext, instance);
    }

    @SuppressWarnings("unchecked")
    private <T> T createPollTask(String name, Object... args) {
        return (T) applicationContext.getBean(name, args);
    }
}
