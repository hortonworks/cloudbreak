package com.sequenceiq.cloudbreak.cloud.template.group;

import static com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup.CANCELLED;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.template.GroupResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.NetworkResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.ResourceNotNeededException;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.template.init.ResourceBuilders;
import com.sequenceiq.cloudbreak.cloud.template.task.ResourcePollTaskFactory;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class GroupResourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroupResourceService.class);

    @Inject
    private ResourceBuilders resourceBuilders;

    @Inject
    private SyncPollingScheduler<List<CloudResourceStatus>> syncPollingScheduler;

    @Inject
    private ResourcePollTaskFactory statusCheckFactory;

    @Inject
    private PersistenceNotifier resourceNotifier;

    public List<CloudResourceStatus> buildResources(ResourceBuilderContext context,
            AuthenticatedContext auth, Iterable<Group> groups, Network network, Security security) throws Exception {
        CloudContext cloudContext = auth.getCloudContext();
        List<CloudResourceStatus> results = new ArrayList<>();
        for (GroupResourceBuilder<ResourceBuilderContext> builder : resourceBuilders.group(cloudContext.getVariant())) {
            PollGroup pollGroup = InMemoryStateStore.getStack(auth.getCloudContext().getId());
            if (CANCELLED.equals(pollGroup)) {
                break;
            }
            for (Group group : getOrderedCopy(groups)) {
                try {
                    CloudResource buildableResource = builder.create(context, auth, group, network);
                    if (buildableResource != null) {
                        buildableResource = CloudResource.builder().cloudResource(buildableResource).group(group.getName()).build();
                        createResource(auth, buildableResource);
                        CloudResource resource = builder.build(context, auth, group, network, group.getSecurity(), buildableResource);
                        updateResource(auth, resource);
                        PollTask<List<CloudResourceStatus>> task = statusCheckFactory.newPollResourceTask(builder, auth, Collections.singletonList(resource),
                                context, true);
                        List<CloudResourceStatus> pollerResult = syncPollingScheduler.schedule(task);
                        context.addGroupResources(group.getName(), Collections.singletonList(resource));
                        results.addAll(pollerResult);
                    } else {
                        LOGGER.debug("CloudResource is null for {} with resourceType: {} and builder: {}, build is skipped.",
                                group.getName(), builder.resourceType(), builder.getClass().getSimpleName());
                    }
                } catch (ResourceNotNeededException e) {
                    LOGGER.debug("Skipping resource creation: {}", e.getMessage());
                }
            }
        }
        return results;
    }

    public List<CloudResourceStatus> deleteResources(ResourceBuilderContext context,
            AuthenticatedContext auth, Collection<CloudResource> resources, Network network, boolean cancellable) throws Exception {
        CloudContext cloudContext = auth.getCloudContext();
        List<CloudResourceStatus> results = new ArrayList<>();
        List<GroupResourceBuilder<ResourceBuilderContext>> builderChain = resourceBuilders.group(cloudContext.getVariant());
        for (int i = builderChain.size() - 1; i >= 0; i--) {
            GroupResourceBuilder<ResourceBuilderContext> builder = builderChain.get(i);
            List<CloudResource> specificResources = getResources(resources, builder.resourceType());
            for (CloudResource resource : specificResources) {
                if (resource.getStatus().resourceExists()) {
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
            Network network, Security security, Collection<CloudResource> groupResources) throws Exception {
        List<CloudResourceStatus> results = new ArrayList<>();
        CloudContext cloudContext = auth.getCloudContext();
        for (NetworkResourceBuilder<ResourceBuilderContext> builder : resourceBuilders.network(cloudContext.getVariant())) {
            List<CloudResource> resources = getResources(groupResources, builder.resourceType());
            for (CloudResource resource : resources) {
                CloudResourceStatus status = builder.update(context, auth, network, security, resource);
                if (status != null) {
                    PollTask<List<CloudResourceStatus>> task = statusCheckFactory.newPollResourceTask(
                            builder, auth, Collections.singletonList(status.getCloudResource()), context, true);
                    List<CloudResourceStatus> pollerResult = syncPollingScheduler.schedule(task);
                    results.addAll(pollerResult);
                }
            }
        }
        return results;
    }

    public List<CloudResource> getGroupResources(Variant variant, Collection<CloudResource> resources) {
        Collection<ResourceType> types = new ArrayList<>();
        for (GroupResourceBuilder<?> builder : resourceBuilders.group(variant)) {
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

    private List<CloudResource> getResources(Collection<CloudResource> resources, ResourceType type) {
        return getResources(resources, Collections.singletonList(type));
    }

    private List<CloudResource> getResources(Collection<CloudResource> resources, Collection<ResourceType> types) {
        List<CloudResource> filtered = new ArrayList<>(resources.size());
        for (CloudResource resource : resources) {
            if (types.contains(resource.getType())) {
                filtered.add(resource);
            }
        }
        return filtered;
    }

    private Iterable<Group> getOrderedCopy(Iterable<Group> groups) {
        Ordering<Group> byLengthOrdering = new Ordering<>() {
            @Override
            public int compare(Group left, Group right) {
                return Ints.compare(left.getInstancesSize(), right.getInstancesSize());
            }
        };
        return byLengthOrdering.sortedCopy(groups);
    }
}
