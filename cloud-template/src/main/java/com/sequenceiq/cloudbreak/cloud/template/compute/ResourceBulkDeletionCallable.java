package com.sequenceiq.cloudbreak.cloud.template.compute;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.template.task.ResourcePollTaskFactory;

public class ResourceBulkDeletionCallable implements Callable<List<CloudResourceStatus>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceBulkDeletionCallable.class);

    private final SyncPollingScheduler<List<CloudResourceStatus>> syncPollingScheduler;

    private final ResourcePollTaskFactory resourcePollTaskFactory;

    private final PersistenceNotifier persistenceNotifier;

    private final ResourceBuilderContext context;

    private final AuthenticatedContext auth;

    private final List<CloudResourceStatus> deletionStatuses;

    private final ComputeResourceBuilder<ResourceBuilderContext> builder;

    private final boolean cancellable;

    public ResourceBulkDeletionCallable(ResourceBulkDeletionCallablePayload payload, SyncPollingScheduler<List<CloudResourceStatus>> syncPollingScheduler,
            ResourcePollTaskFactory resourcePollTaskFactory, PersistenceNotifier persistenceNotifier) {
        this.syncPollingScheduler = syncPollingScheduler;
        this.resourcePollTaskFactory = resourcePollTaskFactory;
        this.persistenceNotifier = persistenceNotifier;
        this.context = payload.getContext();
        this.auth = payload.getAuth();
        this.deletionStatuses = payload.getDeletionStatuses();
        this.builder = payload.getBuilder();
        this.cancellable = payload.isCancellable();
    }

    @Override
    public List<CloudResourceStatus> call() throws Exception {
        LOGGER.debug("Finishing the bulk deletion of {} compute resources: {}", deletionStatuses.size(), deletionStatuses);
        List<CloudResourceStatus> results = new ArrayList<>();
        List<CloudResource> resourcesToPoll = new ArrayList<>();
        for (CloudResourceStatus deletionStatus : deletionStatuses) {
            switch (deletionStatus.getStatus()) {
                case IN_PROGRESS -> resourcesToPoll.add(deletionStatus.getCloudResource());
                case DELETED -> {
                    deleteResource(deletionStatus.getCloudResource());
                    results.add(deletionStatus);
                }
                default -> results.add(deletionStatus);
            }
        }
        if (!resourcesToPoll.isEmpty()) {
            PollTask<List<CloudResourceStatus>> task = resourcePollTaskFactory.newPollResourceTask(builder, auth, resourcesToPoll, context, cancellable);
            results.addAll(syncPollingScheduler.schedule(task));
            resourcesToPoll.forEach(this::deleteResource);
        }
        return results;
    }

    private void deleteResource(CloudResource resource) {
        persistenceNotifier.notifyDeletion(resource, auth.getCloudContext());
    }
}
