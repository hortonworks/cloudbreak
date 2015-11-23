package com.sequenceiq.cloudbreak.cloud.template.compute;

import static com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup.CANCELLED;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.init.ResourceBuilders;
import com.sequenceiq.cloudbreak.cloud.template.task.ResourcePollTaskFactory;

@Component(ResourceCreateThread.NAME)
@Scope(value = "prototype")
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

    private final long privateId;
    private final Group group;
    private final ResourceBuilderContext context;
    private final AuthenticatedContext auth;
    private final Image image;

    public ResourceCreateThread(long privateId, Group group, ResourceBuilderContext context, AuthenticatedContext auth, Image image) {
        this.privateId = privateId;
        this.group = group;
        this.context = context;
        this.auth = auth;
        this.image = image;
    }

    @Override
    public ResourceRequestResult<List<CloudResourceStatus>> call() throws Exception {
        List<CloudResourceStatus> results = new ArrayList<>();
        List<CloudResource> buildableResources = new ArrayList<>();
        try {
            for (ComputeResourceBuilder builder : resourceBuilders.compute(auth.getCloudContext().getPlatform())) {
                LOGGER.info("Building {} resources of {} instance group", builder.resourceType(), group.getName());
                List<CloudResource> list = builder.create(context, privateId, auth, group, image);
                buildableResources.addAll(list);
                createResource(auth, list);

                PollGroup pollGroup = InMemoryStateStore.get(auth.getCloudContext().getId());
                if (pollGroup != null && CANCELLED.equals(pollGroup)) {
                    throw new CancellationException(format("Building of %s has been cancelled", list));
                }

                List<CloudResource> resources = builder.build(context, privateId, auth, group, image, list);
                updateResource(auth, resources);
                context.addComputeResources(privateId, resources);
                PollTask<List<CloudResourceStatus>> task = resourcePollTaskFactory.newPollResourceTask(builder, auth, resources, context, true);
                List<CloudResourceStatus> pollerResult = syncPollingScheduler.schedule(task);
                for (CloudResourceStatus resourceStatus : pollerResult) {
                    resourceStatus.setPrivateId(privateId);
                }
                results.addAll(pollerResult);
            }
        } catch (CancellationException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("", e);
            results.clear();
            for (CloudResource buildableResource : buildableResources) {
                results.add(new CloudResourceStatus(buildableResource, ResourceStatus.FAILED, e.getMessage(), privateId));
            }
            return new ResourceRequestResult<>(FutureResult.FAILED, results);
        }
        return new ResourceRequestResult<>(FutureResult.SUCCESS, results);
    }

    private List<CloudResource> createResource(AuthenticatedContext auth, List<CloudResource> cloudResources) throws Exception {
        for (CloudResource cloudResource : cloudResources) {
            if (cloudResource.isPersistent()) {
                resourceNotifier.notifyAllocation(cloudResource, auth.getCloudContext());
            }
        }
        return cloudResources;
    }

    private List<CloudResource> updateResource(AuthenticatedContext auth, List<CloudResource> cloudResources) throws Exception {
        for (CloudResource cloudResource : cloudResources) {
            if (cloudResource.isPersistent()) {
                resourceNotifier.notifyUpdate(cloudResource, auth.getCloudContext());
            }
        }
        return cloudResources;
    }
}
