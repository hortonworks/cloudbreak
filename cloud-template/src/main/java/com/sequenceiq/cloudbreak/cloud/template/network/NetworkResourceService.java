package com.sequenceiq.cloudbreak.cloud.template.network;

import static com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup.CANCELLED;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.template.NetworkResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.ResourceNotNeededException;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.template.init.ResourceBuilders;
import com.sequenceiq.cloudbreak.cloud.template.task.ResourcePollTaskFactory;
import com.sequenceiq.cloudbreak.common.type.CommonStatus;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@Service
public class NetworkResourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkResourceService.class);

    @Inject
    private ResourceBuilders resourceBuilders;

    @Inject
    private SyncPollingScheduler<List<CloudResourceStatus>> syncPollingScheduler;

    @Inject
    private ResourcePollTaskFactory statusCheckFactory;

    @Inject
    private PersistenceNotifier resourceNotifier;

    public List<CloudResourceStatus> buildResources(ResourceBuilderContext context,
            AuthenticatedContext auth, Network network, Security security) throws Exception {
        CloudContext cloudContext = auth.getCloudContext();
        List<CloudResourceStatus> results = new ArrayList<>();
        for (NetworkResourceBuilder<ResourceBuilderContext> builder : resourceBuilders.network(cloudContext.getPlatform())) {
            PollGroup pollGroup = InMemoryStateStore.getStack(auth.getCloudContext().getId());
            if (CANCELLED.equals(pollGroup)) {
                break;
            }
            try {
                CloudResource buildableResource = builder.create(context, auth, network);
                createResource(auth, buildableResource);
                CloudResource resource = builder.build(context, auth, network, security, buildableResource);
                updateResource(auth, resource);
                PollTask<List<CloudResourceStatus>> task = statusCheckFactory.newPollResourceTask(builder, auth,
                        Collections.singletonList(resource), context, true);
                List<CloudResourceStatus> pollerResult = syncPollingScheduler.schedule(task);
                results.addAll(pollerResult);
            } catch (ResourceNotNeededException e) {
                LOGGER.debug("Skipping resource creation: {}", e.getMessage());
            }
        }
        return results;
    }

    public List<CloudResourceStatus> deleteResources(ResourceBuilderContext context,
            AuthenticatedContext auth, Iterable<CloudResource> resources, Network network, boolean cancellable) throws Exception {
        CloudContext cloudContext = auth.getCloudContext();
        List<CloudResourceStatus> results = new ArrayList<>();
        List<NetworkResourceBuilder<ResourceBuilderContext>> builderChain = resourceBuilders.network(cloudContext.getPlatform());
        for (int i = builderChain.size() - 1; i >= 0; i--) {
            NetworkResourceBuilder<ResourceBuilderContext> builder = builderChain.get(i);
            List<CloudResource> specificResources = getResources(resources, builder.resourceType());
            for (CloudResource resource : specificResources) {
                if (resource.getStatus() == CommonStatus.CREATED) {
                    CloudResource deletedResource = builder.delete(context, auth, resource, network);
                    if (deletedResource != null) {
                        PollTask<List<CloudResourceStatus>> task = statusCheckFactory.newPollResourceTask(
                                builder, auth, Collections.singletonList(deletedResource), context, cancellable);
                        List<CloudResourceStatus> pollerResult = syncPollingScheduler.schedule(task);
                        results.addAll(pollerResult);
                    }
                }
                resourceNotifier.notifyDeletion(resource, cloudContext);
            }
        }
        return results;
    }

    public List<CloudResourceStatus> update(ResourceBuilderContext context, AuthenticatedContext auth,
            Network network, Security security, Iterable<CloudResource> networkResources) throws Exception {
        List<CloudResourceStatus> results = new ArrayList<>();
        CloudContext cloudContext = auth.getCloudContext();
        for (NetworkResourceBuilder<ResourceBuilderContext> builder : resourceBuilders.network(cloudContext.getPlatform())) {
            CloudResource resource = getResources(networkResources, builder.resourceType()).get(0);
            CloudResourceStatus status = builder.update(context, auth, network, security, resource);
            if (status != null) {
                PollTask<List<CloudResourceStatus>> task = statusCheckFactory.newPollResourceTask(
                        builder, auth, Collections.singletonList(status.getCloudResource()), context, true);
                List<CloudResourceStatus> pollerResult = syncPollingScheduler.schedule(task);
                results.addAll(pollerResult);
            }
        }
        return results;
    }

    public List<CloudResource> getNetworkResources(Platform platform, Iterable<CloudResource> resources) {
        Collection<ResourceType> types = new ArrayList<>();
        for (NetworkResourceBuilder<?> builder : resourceBuilders.network(platform)) {
            types.add(builder.resourceType());
        }
        return getResources(resources, types);
    }

    protected void createResource(AuthenticatedContext auth, CloudResource buildableResource) {
        if (buildableResource.isPersistent()) {
            resourceNotifier.notifyAllocation(buildableResource, auth.getCloudContext());
        }
    }

    protected void updateResource(AuthenticatedContext auth, CloudResource buildableResource) {
        if (buildableResource.isPersistent()) {
            resourceNotifier.notifyUpdate(buildableResource, auth.getCloudContext());
        }
    }

    private List<CloudResource> getResources(Iterable<CloudResource> resources, ResourceType type) {
        return getResources(resources, Collections.singletonList(type));
    }

    private List<CloudResource> getResources(Iterable<CloudResource> resources, Collection<ResourceType> types) {
        List<CloudResource> filtered = new ArrayList<>();
        for (CloudResource resource : resources) {
            if (types.contains(resource.getType())) {
                filtered.add(resource);
            }
        }
        return filtered;
    }
}
