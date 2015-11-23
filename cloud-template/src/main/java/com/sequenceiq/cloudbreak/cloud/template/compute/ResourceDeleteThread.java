package com.sequenceiq.cloudbreak.cloud.template.compute;

import static java.util.Arrays.asList;

import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudNotificationException;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.task.ResourcePollTaskFactory;
import com.sequenceiq.cloudbreak.common.type.CommonStatus;

@Component(ResourceDeleteThread.NAME)
@Scope(value = "prototype")
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
    private final ComputeResourceBuilder builder;
    private final boolean cancellable;

    public ResourceDeleteThread(ResourceBuilderContext context, AuthenticatedContext auth,
            CloudResource resource, ComputeResourceBuilder builder, boolean cancellable) {
        this.context = context;
        this.auth = auth;
        this.resource = resource;
        this.builder = builder;
        this.cancellable = cancellable;
    }

    @Override
    public ResourceRequestResult<List<CloudResourceStatus>> call() throws Exception {
        LOGGER.info("Deleting compute resource {}", resource);
        if (resource.getStatus() == CommonStatus.CREATED) {
            CloudResource deletedResource = builder.delete(context, auth, resource);
            if (deletedResource != null) {
                PollTask<List<CloudResourceStatus>> task = resourcePollTaskFactory
                        .newPollResourceTask(builder, auth, asList(deletedResource), context, cancellable);
                List<CloudResourceStatus> pollerResult = syncPollingScheduler.schedule(task);
                deleteResource();
                return new ResourceRequestResult<>(FutureResult.SUCCESS, pollerResult);
            }
        }
        deleteResource();
        CloudResourceStatus status = new CloudResourceStatus(resource, ResourceStatus.DELETED);
        return new ResourceRequestResult<>(FutureResult.SUCCESS, asList(status));
    }

    private void deleteResource() throws InterruptedException, CloudNotificationException {
        resourceNotifier.notifyDeletion(resource, auth.getCloudContext());
    }

}
