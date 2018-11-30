package com.sequenceiq.cloudbreak.cloud.template.compute;

import static com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup.CANCELLED;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.template.init.ResourceBuilders;
import com.sequenceiq.cloudbreak.cloud.template.task.ResourcePollTaskFactory;

@Component(ResourceCreateThread.NAME)
@Scope("prototype")
public class ResourceCreateThread implements Callable<ResourceRequestResult<List<CloudResourceStatus>>> {

    public static final String NAME = "resourceCreateThread";

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceCreateThread.class);

    @Inject
    private ResourceBuilders resourceBuilders;

    @Inject
    private SyncPollingScheduler<List<CloudResourceStatus>> syncPollingScheduler;

    @Inject
    private ResourcePollTaskFactory resourcePollTaskFactory;

    @Inject
    private PersistenceNotifier resourceNotifier;

    private final Long privateId;

    private final Group group;

    private final ResourceBuilderContext context;

    private final AuthenticatedContext auth;

    private final CloudStack cloudStack;

    public ResourceCreateThread(long privateId, Group group, ResourceBuilderContext context, AuthenticatedContext auth, CloudStack cloudStack) {
        this.privateId = privateId;
        this.group = group;
        this.context = context;
        this.auth = auth;
        this.cloudStack = cloudStack;
    }

    @Override
    public ResourceRequestResult<List<CloudResourceStatus>> call() {
        List<CloudResourceStatus> results = new ArrayList<>();
        Collection<CloudResource> buildableResources = new ArrayList<>();
        try {
            List<ComputeResourceBuilder> compute = resourceBuilders.compute(auth.getCloudContext().getPlatform());
            for (ComputeResourceBuilder builder : compute) {
                LOGGER.info("Building {} resources of {} instance group", builder.resourceType(), group.getName());
                List<CloudResource> cloudResources = builder.create(context, privateId, auth, group, cloudStack.getImage());
                if (Objects.nonNull(cloudResources) && !cloudResources.isEmpty()) {
                    buildableResources.addAll(cloudResources);
                    createResource(auth, cloudResources);

                    PollGroup pollGroup = InMemoryStateStore.getStack(auth.getCloudContext().getId());
                    if (pollGroup != null && CANCELLED.equals(pollGroup)) {
                        throw new CancellationException(format("Building of %s has been cancelled", cloudResources));
                    }

                    List<CloudResource> resources = builder.build(context, privateId, auth, group, cloudResources, cloudStack);
                    updateResource(auth, resources);
                    context.addComputeResources(privateId, resources);
                    PollTask<List<CloudResourceStatus>> task = resourcePollTaskFactory.newPollResourceTask(builder, auth, resources, context, true);
                    List<CloudResourceStatus> pollerResult = syncPollingScheduler.schedule(task);
                    for (CloudResourceStatus resourceStatus : pollerResult) {
                        resourceStatus.setPrivateId(privateId);
                    }
                    results.addAll(pollerResult);
                }
            }
        } catch (CancellationException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.warn("", e);
            results.clear();
            for (CloudResource buildableResource : buildableResources) {
                results.add(new CloudResourceStatus(buildableResource, ResourceStatus.FAILED, e.getMessage(), privateId));
            }
            return new ResourceRequestResult<>(FutureResult.FAILED, results);
        }
        return new ResourceRequestResult<>(FutureResult.SUCCESS, results);
    }

    private void createResource(AuthenticatedContext auth, Iterable<CloudResource> cloudResources) {
        for (CloudResource cloudResource : cloudResources) {
            if (cloudResource.isPersistent()) {
                resourceNotifier.notifyAllocation(cloudResource, auth.getCloudContext());
            }
        }
    }

    private void updateResource(AuthenticatedContext auth, Iterable<CloudResource> cloudResources) {
        for (CloudResource cloudResource : cloudResources) {
            if (cloudResource.isPersistent()) {
                resourceNotifier.notifyUpdate(cloudResource, auth.getCloudContext());
            }
        }
    }
}
