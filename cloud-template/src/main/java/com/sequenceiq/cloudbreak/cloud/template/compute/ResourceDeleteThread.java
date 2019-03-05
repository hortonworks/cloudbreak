package com.sequenceiq.cloudbreak.cloud.template.compute;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

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
import com.sequenceiq.cloudbreak.common.type.CommonStatus;

@Component(ResourceDeleteThread.NAME)
@Scope("prototype")
public class ResourceDeleteThread implements Callable<ResourceRequestResult<List<CloudResourceStatus>>> {

    public static final String NAME = "resourceDeleteThread";

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceDeleteThread.class);

    @Inject
    private SyncPollingScheduler<List<CloudResourceStatus>> syncPollingScheduler;

    @Inject
    private ResourcePollTaskFactory resourcePollTaskFactory;

    @Inject
    private PersistenceNotifier resourceNotifier;

    private final ResourceBuilderContext context;

    private final AuthenticatedContext auth;

    private final CloudResource resource;

    private final ComputeResourceBuilder<ResourceBuilderContext> builder;

    private final boolean cancellable;

    public ResourceDeleteThread(ResourceBuilderContext context, AuthenticatedContext auth,
            CloudResource resource, ComputeResourceBuilder<ResourceBuilderContext> builder, boolean cancellable) {
        this.context = context;
        this.auth = auth;
        this.resource = resource;
        this.builder = builder;
        this.cancellable = cancellable;
    }

    @Override
    public ResourceRequestResult<List<CloudResourceStatus>> call() throws Exception {
        LOGGER.debug("Deleting compute resource {}", resource);
        if (resource.getStatus() == CommonStatus.CREATED) {
            CloudResource deletedResource;
            try {
                deletedResource = builder.delete(context, auth, resource);
            } catch (InterruptedException ignored) {
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
        resourceNotifier.notifyDeletion(resource, auth.getCloudContext());
    }

}
