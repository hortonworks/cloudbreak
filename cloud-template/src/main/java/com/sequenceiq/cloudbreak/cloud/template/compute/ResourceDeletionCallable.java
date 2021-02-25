package com.sequenceiq.cloudbreak.cloud.template.compute;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.template.task.ResourcePollTaskFactory;

public class ResourceDeletionCallable implements Callable<ResourceRequestResult<List<CloudResourceStatus>>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceDeletionCallable.class);

    private final SyncPollingScheduler<List<CloudResourceStatus>> syncPollingScheduler;

    private final ResourcePollTaskFactory resourcePollTaskFactory;

    private final PersistenceNotifier persistenceNotifier;

    private final ResourceBuilderContext context;

    private final AuthenticatedContext auth;

    private final CloudResource resource;

    private final ComputeResourceBuilder<ResourceBuilderContext> builder;

    private final boolean cancellable;

    public ResourceDeletionCallable(ResourceDeletionCallablePayload payload, SyncPollingScheduler<List<CloudResourceStatus>> syncPollingScheduler,
            ResourcePollTaskFactory resourcePollTaskFactory, PersistenceNotifier persistenceNotifier) {
        this.syncPollingScheduler = syncPollingScheduler;
        this.resourcePollTaskFactory = resourcePollTaskFactory;
        this.persistenceNotifier = persistenceNotifier;
        this.context = payload.getContext();
        this.auth = payload.getAuth();
        this.resource = payload.getResource();
        this.builder = payload.getBuilder();
        this.cancellable = payload.isCancellable();
    }

    @Override
    public ResourceRequestResult<List<CloudResourceStatus>> call() throws Exception {
        LOGGER.debug("Deleting compute resource {}", resource);
        if (resource.getStatus().resourceExists()) {
            CloudResource deletedResource;
            try {
                deletedResource = builder.delete(context, auth, resource);
            } catch (PreserveResourceException ignored) {
                LOGGER.debug("Preserve resource for later use.");
                CloudResourceStatus status = new CloudResourceStatus(resource, ResourceStatus.CREATED);
                return new ResourceRequestResult<>(FutureResult.SUCCESS, Collections.singletonList(status));
            }
            if (deletedResource != null) {
                PollTask<List<CloudResourceStatus>> task = resourcePollTaskFactory
                        .newPollResourceTask(builder, auth, Collections.singletonList(deletedResource), context, cancellable);
                List<CloudResourceStatus> pollerResult = syncPollingScheduler.schedule(task);
                deleteResource();
                return new ResourceRequestResult<>(FutureResult.SUCCESS, pollerResult);
            }
        }
        deleteResource();
        CloudResourceStatus status = new CloudResourceStatus(resource, ResourceStatus.DELETED);
        return new ResourceRequestResult<>(FutureResult.SUCCESS, Collections.singletonList(status));
    }

    private void deleteResource() {
        persistenceNotifier.notifyDeletion(resource, auth.getCloudContext());
    }

}
