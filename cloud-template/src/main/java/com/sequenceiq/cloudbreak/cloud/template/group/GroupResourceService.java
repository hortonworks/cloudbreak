package com.sequenceiq.cloudbreak.cloud.template.group;

import static com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup.CANCELLED;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

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
                for (String availabilityZone : getAvailabilityZones(builder, group, context)) {
                    results.addAll(buildResource(context, auth, network, builder, group, availabilityZone));
                }
            }
        }
        return results;
    }

    /**
     * Builds the zonal group resources that are missing for the availability zones of the given groups,
     * e.g. when a group was extended to new zones after it had been provisioned.
     * A zone counts as covered when a resource of the same type already exists for the group in that zone, regardless of its name,
     * so resources created with legacy naming are never duplicated.
     * Group resources keep their REQUESTED status after a successful build, so a row left behind by a failed build can't be told apart
     * from a built one. The resources of covered zones are built again instead, which relies on the build of zonal resources being idempotent,
     * so a resource that is missing on the provider is recreated.
     * Stacks that have no resource of a zonal type at all predate that resource type, so there is nothing to extend and they are left as they are.
     */
    public List<CloudResourceStatus> buildMissingZonalResources(ResourceBuilderContext context, AuthenticatedContext auth, Iterable<Group> groups,
            Network network, Collection<CloudResource> existingResources) throws Exception {
        List<CloudResourceStatus> results = new ArrayList<>();
        for (GroupResourceBuilder<ResourceBuilderContext> builder : resourceBuilders.group(auth.getCloudContext().getVariant())) {
            if (builder.isZonalResource()) {
                results.addAll(buildMissingZonalResourcesOfType(context, auth, groups, network, builder,
                        getResources(existingResources, builder.resourceType())));
            }
        }
        return results;
    }

    private List<CloudResourceStatus> buildMissingZonalResourcesOfType(ResourceBuilderContext context, AuthenticatedContext auth, Iterable<Group> groups,
            Network network, GroupResourceBuilder<ResourceBuilderContext> builder, List<CloudResource> existingResourcesOfType) throws Exception {
        if (CANCELLED.equals(InMemoryStateStore.getStack(auth.getCloudContext().getId()))) {
            LOGGER.info("Stack {} is cancelled, skipping the build of missing {}", auth.getCloudContext().getName(), builder.resourceType());
            return List.of();
        }
        if (existingResourcesOfType.isEmpty()) {
            LOGGER.info("Stack {} has no {}, skipping the build of missing ones", auth.getCloudContext().getName(), builder.resourceType());
            return List.of();
        }
        List<CloudResourceStatus> results = new ArrayList<>();
        for (Group group : getOrderedCopy(groups)) {
            for (String availabilityZone : getAvailabilityZones(builder, group, context)) {
                results.addAll(buildZonalResource(context, auth, network, builder, group, availabilityZone, existingResourcesOfType));
            }
        }
        return results;
    }

    private List<CloudResourceStatus> buildZonalResource(ResourceBuilderContext context, AuthenticatedContext auth, Network network,
            GroupResourceBuilder<ResourceBuilderContext> builder, Group group, String availabilityZone, List<CloudResource> existingResourcesOfType)
            throws Exception {
        Optional<CloudResource> coveringResource = findResourceCoveringZone(existingResourcesOfType, group, availabilityZone, context);
        if (coveringResource.isEmpty()) {
            LOGGER.info("Building missing {} for group {} in availability zone {}", builder.resourceType(), group.getName(), availabilityZone);
            return buildResource(context, auth, network, builder, group, availabilityZone);
        }
        if (coveringResource.get().getAvailabilityZone() == null) {
            LOGGER.debug("Legacy {} {} without availability zone covers group {} in availability zone {}", builder.resourceType(),
                    coveringResource.get().getName(), group.getName(), availabilityZone);
            return List.of();
        }
        LOGGER.debug("{} {} exists for group {} in availability zone {}, building it again in case it is missing on the provider",
                builder.resourceType(), coveringResource.get().getName(), group.getName(), availabilityZone);
        return buildAndPollResource(context, auth, network, builder, group, coveringResource.get());
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

    private List<CloudResourceStatus> buildResource(ResourceBuilderContext context, AuthenticatedContext auth, Network network,
            GroupResourceBuilder<ResourceBuilderContext> builder, Group group, String availabilityZone) throws Exception {
        Optional<CloudResource> buildableResource = createBuildableResource(context, auth, network, builder, group, availabilityZone);
        if (buildableResource.isEmpty()) {
            return List.of();
        }
        return buildAndPollResource(context, auth, network, builder, group, buildableResource.get());
    }

    private Optional<CloudResource> createBuildableResource(ResourceBuilderContext context, AuthenticatedContext auth, Network network,
            GroupResourceBuilder<ResourceBuilderContext> builder, Group group, String availabilityZone) {
        try {
            CloudResource buildableResource = builder.create(context, auth, group, network, availabilityZone);
            if (buildableResource == null) {
                LOGGER.debug("CloudResource is null for {} with resourceType: {} and builder: {}, build is skipped.",
                        group.getName(), builder.resourceType(), builder.getClass().getSimpleName());
                return Optional.empty();
            }
            buildableResource = CloudResource.builder().cloudResource(buildableResource).withGroup(group.getName()).build();
            createResource(auth, buildableResource);
            return Optional.of(buildableResource);
        } catch (ResourceNotNeededException e) {
            LOGGER.debug("Skipping resource creation: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private List<CloudResourceStatus> buildAndPollResource(ResourceBuilderContext context, AuthenticatedContext auth, Network network,
            GroupResourceBuilder<ResourceBuilderContext> builder, Group group, CloudResource buildableResource) throws Exception {
        try {
            CloudResource resource = builder.build(context, auth, group, network, group.getSecurity(), buildableResource);
            updateResource(auth, resource);
            PollTask<List<CloudResourceStatus>> task = statusCheckFactory.newPollResourceTask(builder, auth, Collections.singletonList(resource),
                    context, true);
            List<CloudResourceStatus> pollerResult = syncPollingScheduler.schedule(task);
            context.addGroupResources(group.getName(), Collections.singletonList(resource));
            return pollerResult;
        } catch (ResourceNotNeededException e) {
            LOGGER.debug("Skipping resource creation: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Resources without a group or availability zone predate zonal group resources: they were created for the stack's own zone,
     * and are considered to cover it for every group, so we never create a second resource next to them.
     */
    private Optional<CloudResource> findResourceCoveringZone(List<CloudResource> existingResources, Group group, String availabilityZone,
            ResourceBuilderContext context) {
        String stackAvailabilityZone = context.getLocation().getAvailabilityZone().value();
        return existingResources.stream()
                .filter(resource -> resource.getGroup() == null || resource.getGroup().equals(group.getName()))
                .filter(resource -> availabilityZone.equals(Objects.requireNonNullElse(resource.getAvailabilityZone(), stackAvailabilityZone)))
                .findFirst();
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

    private Set<String> getAvailabilityZones(GroupResourceBuilder<ResourceBuilderContext> builder, Group group, ResourceBuilderContext context) {
        Set<String> availabilityZones = new HashSet<>();
        availabilityZones.add(context.getLocation().getAvailabilityZone().value());
        if (builder.isZonalResource() && !CollectionUtils.isEmpty(group.getNetwork().getAvailabilityZones())) {
            return group.getNetwork().getAvailabilityZones();
        }
        return availabilityZones;
    }
}
