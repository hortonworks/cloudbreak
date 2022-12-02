package com.sequenceiq.cloudbreak.cloud.template.compute;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.template.task.ResourcePollTaskFactory;

public class ResourceUpdateCallable implements Callable<ResourceRequestResult<List<CloudResourceStatus>>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceUpdateCallable.class);

    private final SyncPollingScheduler<List<CloudResourceStatus>> syncPollingScheduler;

    private final ResourcePollTaskFactory resourcePollTaskFactory;

    private final ResourceBuilderContext context;

    private final AuthenticatedContext auth;

    private final CloudResource resource;

    private final CloudInstance instance;

    private final CloudStack stack;

    private final ComputeResourceBuilder<ResourceBuilderContext> builder;

    public ResourceUpdateCallable(ResourceUpdateCallablePayload payload, SyncPollingScheduler<List<CloudResourceStatus>> syncPollingScheduler,
        ResourcePollTaskFactory resourcePollTaskFactory) {
        this.syncPollingScheduler = syncPollingScheduler;
        this.resourcePollTaskFactory = resourcePollTaskFactory;
        this.context = payload.getContext();
        this.auth = payload.getAuth();
        this.resource = payload.getCloudResource();
        this.instance = payload.getCloudInstance();
        this.stack = payload.getCloudStack();
        this.builder = payload.getBuilder();
    }

    @Override
    public ResourceRequestResult<List<CloudResourceStatus>> call() throws Exception {
        LOGGER.debug("Deleting compute resource {}", resource);
        if (resource.getStatus().resourceExists()) {
            CloudResource updateResource;
            try {
                updateResource = builder.update(context, resource, instance, auth, stack);
            } catch (PreserveResourceException ignored) {
                LOGGER.debug("Preserve resource for later use.");
                CloudResourceStatus status = new CloudResourceStatus(resource, ResourceStatus.CREATED);
                return new ResourceRequestResult<>(FutureResult.SUCCESS, Collections.singletonList(status));
            }
            if (updateResource != null) {
                PollTask<List<CloudResourceStatus>> task = resourcePollTaskFactory
                        .newPollResourceTask(builder, auth, Collections.singletonList(updateResource), context, false);
                List<CloudResourceStatus> pollerResult = syncPollingScheduler.schedule(task);
                return new ResourceRequestResult<>(FutureResult.SUCCESS, pollerResult);
            }
        }
        CloudResourceStatus status = new CloudResourceStatus(resource, ResourceStatus.DELETED);
        return new ResourceRequestResult<>(FutureResult.SUCCESS, Collections.singletonList(status));
    }
}
