package com.sequenceiq.cloudbreak.cloud.template.compute;

import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.CREATE_REQUESTED;
import static com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup.CANCELLED;
import static com.sequenceiq.cloudbreak.cloud.template.compute.CloudFailureHandler.ScaleContext;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import com.sequenceiq.cloudbreak.cloud.UpdateType;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.template.init.ResourceBuilders;
import com.sequenceiq.cloudbreak.cloud.template.task.ResourcePollTaskFactory;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class ComputeResourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComputeResourceService.class);

    private static final int MAX_POLLING_ATTEMPT = 100;

    @Inject
    private ResourceBuilders resourceBuilders;

    @Inject
    private CloudFailureHandler cloudFailureHandler;

    @Inject
    private SyncPollingScheduler<List<CloudVmInstanceStatus>> syncVMPollingScheduler;

    @Inject
    private SyncPollingScheduler<List<CloudResourceStatus>> syncPollingScheduler;

    @Inject
    private ResourcePollTaskFactory resourcePollTaskFactory;

    @Inject
    private ResourceActionFactory resourceActionFactory;

    public List<CloudResourceStatus> buildResourcesForLaunch(ResourceBuilderContext ctx, AuthenticatedContext auth, CloudStack cloudStack,
            AdjustmentTypeWithThreshold adjustmentTypeWithThreshold) {
        LOGGER.info("Build compute resources for launch with adjustment type and threshold: {}", adjustmentTypeWithThreshold);
        return new ResourceBuilder(ctx, auth).buildResources(cloudStack, cloudStack.getGroups(), false, adjustmentTypeWithThreshold);
    }

    public List<CloudResourceStatus> buildResourcesForUpscale(ResourceBuilderContext ctx, AuthenticatedContext auth, CloudStack cloudStack,
            Iterable<Group> groups, AdjustmentTypeWithThreshold adjustmentTypeWithThreshold) {
        LOGGER.info("Build compute resources for upscale with adjustment type and threshold: {}", adjustmentTypeWithThreshold);
        return new ResourceBuilder(ctx, auth).buildResources(cloudStack, groups, true, adjustmentTypeWithThreshold);
    }

    public List<CloudResourceStatus> deleteResources(ResourceBuilderContext context, AuthenticatedContext auth,
            Iterable<CloudResource> resources, boolean cancellable, boolean hardFail) {
        LOGGER.debug("Deleting the following resources: {}", resources);
        List<CloudResourceStatus> results = new ArrayList<>();
        Collection<Future<List<CloudResourceStatus>>> futures = new ArrayList<>();
        Variant variant = auth.getCloudContext().getVariant();
        List<ComputeResourceBuilder<ResourceBuilderContext>> builders = Lists.reverse(resourceBuilders.compute(variant));
        int poolSize = context.getResourceBuilderPoolSize();
        LOGGER.info("Pool size: {} for delete resources", poolSize);
        try (ExecutorService executor = new ThreadPoolExecutor(0, poolSize, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(), Thread.ofVirtual().factory())) {
            for (ComputeResourceBuilder<ResourceBuilderContext> builder : builders) {
                LOGGER.debug("Delete resources: execute {} builder for the resource of {}", builder.getClass().getSimpleName(), builder.resourceType());
                List<CloudResource> resourceList = getResources(builder.resourceType(), resources);
                for (CloudResource cloudResource : resourceList) {
                    ResourceDeletionCallable deletionCallable = resourceActionFactory.buildDeletionCallable(
                            new ResourceDeletionCallablePayload(context, auth, cloudResource, builder, cancellable));
                    futures.add(executor.submit(deletionCallable));
                    results.addAll(getDeletedResourcesOrFail(futures, hardFail));
                }
                // wait for builder type to finish before starting the next one
                results.addAll(getDeletedResourcesOrFail(futures, hardFail));
            }
        }
        return results;
    }

    private List<CloudResourceStatus> getDeletedResourcesOrFail(Collection<Future<List<CloudResourceStatus>>> futures,
            boolean hardFail) {
        List<CloudResourceStatus> cloudResourceStatuses = getBuilderResults(futures);
        List<CloudResourceStatus> failedResources = cloudResourceStatuses.stream()
                .filter(cloudResourceStatus -> ResourceStatus.FAILED.equals(cloudResourceStatus.getStatus()))
                .toList();
        if (hardFail && !failedResources.isEmpty()) {
            throw new CloudConnectorException("Resource deletion failed. Reason: "
                    + failedResources.stream().map(CloudResourceStatus::getStatusReason).collect(Collectors.joining(" ")));
        }
        return cloudResourceStatuses;
    }

    public List<CloudResourceStatus> update(ResourceBuilderContext ctx, AuthenticatedContext auth, CloudStack stack,
            List<CloudResource> cloudResource, Optional<String> group, UpdateType updateType) {
        LOGGER.info("Update compute resources.");
        return new ResourceBuilder(ctx, auth).updateResources(ctx, auth, cloudResource, stack, group, updateType);
    }

    public List<CloudVmInstanceStatus> stopInstances(ResourceBuilderContext context, AuthenticatedContext auth, List<CloudInstance> cloudInstances) {
        return stopStart(context, auth, cloudInstances);
    }

    public List<CloudVmInstanceStatus> startInstances(ResourceBuilderContext context, AuthenticatedContext auth, List<CloudInstance> cloudInstances) {
        return stopStart(context, auth, cloudInstances);
    }

    private List<CloudVmInstanceStatus> stopStart(ResourceBuilderContext context,
            AuthenticatedContext auth, List<CloudInstance> instances) {
        LOGGER.debug("Stop / start instances: {}", instances);
        List<CloudVmInstanceStatus> results = new ArrayList<>();
        Variant variant = auth.getCloudContext().getVariant();
        List<ComputeResourceBuilder<ResourceBuilderContext>> builders = resourceBuilders.compute(variant);
        if (!context.isBuild()) {
            Collections.reverse(builders);
        }

        List<ComputeResourceBuilder<ResourceBuilderContext>> instanceBuilders =
                builders.stream().filter(ComputeResourceBuilder::isInstanceBuilder).toList();

        int poolSize = context.getResourceBuilderPoolSize();
        LOGGER.info("Pool size: {} for stop / start", poolSize);
        try (ExecutorService executor = new ThreadPoolExecutor(0, poolSize, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(), Thread.ofVirtual().factory())) {
            for (ComputeResourceBuilder<ResourceBuilderContext> builder : instanceBuilders) {
                LOGGER.debug("Stop / start resources: execute {} builder for the resource of {}", builder.getClass().getSimpleName(), builder.resourceType());
                if (!instances.isEmpty()) {
                    Collection<Future<List<CloudVmInstanceStatus>>> futures = new ArrayList<>();
                    for (CloudInstance instance : instances) {
                        ResourceStopStartCallablePayload stopStartCallablePayload =
                                new ResourceStopStartCallablePayload(context, auth, List.of(instance), builder);
                        ResourceStopStartCallable stopStartCallable = resourceActionFactory.buildStopStartCallable(stopStartCallablePayload);
                        Future<List<CloudVmInstanceStatus>> future = executor.submit(stopStartCallable);
                        futures.add(future);
                    }

                    LOGGER.debug("Wait for all {} stop/start threads to finish", futures.size());
                    List<List<CloudVmInstanceStatus>> instancesStatuses = waitForRequests(futures);
                    LOGGER.debug("There are {} success operation. Instance statuses: {}", instancesStatuses.size(), instancesStatuses);
                    List<CloudVmInstanceStatus> allVmStatuses = instancesStatuses.stream().flatMap(Collection::stream).toList();
                    List<CloudInstance> checkInstances = allVmStatuses.stream().map(CloudVmInstanceStatus::getCloudInstance).collect(Collectors.toList());
                    PollTask<List<CloudVmInstanceStatus>> pollTask =
                            resourcePollTaskFactory.newPollComputeStatusTask(builder, auth, context, checkInstances);
                    try {
                        List<CloudVmInstanceStatus> statuses = syncVMPollingScheduler.schedule(pollTask, MAX_POLLING_ATTEMPT);
                        results.addAll(statuses);
                    } catch (Exception e) {
                        LOGGER.debug("Failed to poll the instances status of {}, set the status to failed", checkInstances, e);
                        results.addAll(allVmStatuses.stream()
                                .map(vs -> new CloudVmInstanceStatus(vs.getCloudInstance(), InstanceStatus.FAILED, e.getMessage()))
                                .toList());
                    }
                } else {
                    LOGGER.debug("Cloud resources are not instances so they cannot be stopped or started, skipping builder type {}", builder.resourceType());
                }
            }
        }
        return results;
    }

    private <T> List<T> waitForRequests(Collection<Future<T>> futures) {
        List<T> resultList = new ArrayList<>();
        int requests = futures.size();
        LOGGER.debug("Waiting for {} requests to finish", requests);
        try {
            for (Future<T> future : futures) {
                resultList.add(future.get());
            }
        } catch (InterruptedException e) {
            LOGGER.error("The waiting for ResourceRequestResults has been interrupted:", e);
        } catch (ExecutionException e) {
            String causeMessage = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            String msg = "The execution of infrastructure operations failed: " + causeMessage;
            LOGGER.warn(msg, e);
            throw new CloudConnectorException(msg, e);
        } finally {
            futures.clear();
        }
        LOGGER.debug("{} requests have finished, continue with next group", requests);
        return resultList;
    }

    private List<CloudResource> getResources(ResourceType resourceType, Iterable<CloudResource> resources) {
        List<CloudResource> selected = new ArrayList<>();
        for (CloudResource resource : resources) {
            if (resourceType == resource.getType()) {
                selected.add(resource);
            }
        }
        return selected;
    }

    public List<CloudResource> getComputeResources(Variant variant, Iterable<CloudResource> resources) {
        Collection<ResourceType> types = new ArrayList<>();
        for (ComputeResourceBuilder<?> builder : resourceBuilders.compute(variant)) {
            types.add(builder.resourceType());
        }
        return getResources(resources, types);
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

    private int getNewNodeCount(Iterable<Group> groups) {
        int fullNodeCount = 0;
        for (Group group : groups) {
            fullNodeCount += (long) group.getInstances().stream()
                    .filter(cloudInstance -> CREATE_REQUESTED.equals(cloudInstance.getTemplate().getStatus()))
                    .count();
        }
        return fullNodeCount;
    }

    private List<CloudResourceStatus> getBuilderResults(Collection<Future<List<CloudResourceStatus>>> futures) {
        return waitForRequests(futures).stream().flatMap(Collection::stream).toList();
    }

    private class ResourceBuilder {

        private final ResourceBuilderContext ctx;

        private final AuthenticatedContext auth;

        ResourceBuilder(ResourceBuilderContext ctx, AuthenticatedContext auth) {
            this.ctx = ctx;
            this.auth = auth;
        }

        public List<CloudResourceStatus> buildResources(CloudStack cloudStack, Iterable<Group> groups,
                Boolean upscale, AdjustmentTypeWithThreshold adjustmentTypeAndThreshold) {
            int poolSize = ctx.getResourceBuilderPoolSize();
            LOGGER.info("Pool size: {} for resource creation", poolSize);
            try (ExecutorService executor = new ThreadPoolExecutor(0, poolSize, 0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(), Thread.ofVirtual().factory())) {
                List<Future<List<CloudResourceStatus>>> futures = new ArrayList<>();
                for (Group group : groups) {
                    LOGGER.debug("Build resources for group: {}", group.getName());
                    List<CloudInstance> createRequestedInstances = group.getInstances().stream()
                            .filter(cloudInstance -> CREATE_REQUESTED.equals(cloudInstance.getTemplate().getStatus())).toList();
                    for (CloudInstance instance : createRequestedInstances) {
                        LOGGER.debug("Build resources for instance: {}", instance.getInstanceId());
                        ResourceCreationCallablePayload creationCallablePayload =
                                new ResourceCreationCallablePayload(List.of(instance), group, ctx, auth, cloudStack);
                        ResourceCreationCallable creationCallable = resourceActionFactory.buildCreationCallable(creationCallablePayload);
                        Future<List<CloudResourceStatus>> future = executor.submit(creationCallable);
                        futures.add(future);
                    }
                }

                List<CloudResourceStatus> results = waitForResourceCreations(groups, upscale, adjustmentTypeAndThreshold, futures);
                LOGGER.debug("Resource creation has been finished, the results are: {}", results);
                return results;
            }
        }

        private List<CloudResourceStatus> waitForResourceCreations(Iterable<Group> groups, Boolean upscale,
                AdjustmentTypeWithThreshold adjustmentTypeAndThreshold, Collection<Future<List<CloudResourceStatus>>> futures) {
            List<CloudResourceStatus> results = new ArrayList<>();
            if (!futures.isEmpty()) {
                LOGGER.debug("Wait for all {} creation threads to finish", futures.size());
                List<List<CloudResourceStatus>> futureResults = waitForRequests(futures);
                List<CloudResourceStatus> resourceStatuses = waitForResourceCreations(futureResults);
                List<CloudResourceStatus> failedResources = filterResourceStatuses(resourceStatuses, ResourceStatus.FAILED);

                Map<String, Group> groupMapByName = StreamSupport.stream(groups.spliterator(), false).collect(Collectors.toMap(Group::getName, group -> group));

                Map<CloudResourceStatus, Group> groupCloudResourceStatuses = resourceStatuses.stream().distinct()
                        .filter(status -> status.getCloudResource().getGroup() != null)
                        .collect(Collectors.toMap(status -> status, status -> groupMapByName.get(status.getCloudResource().getGroup())));

                Map<CloudResourceStatus, Group> failedGroupCloudResourceStatus = failedResources.stream().distinct()
                        .collect(Collectors.toMap(status -> status, status -> groupMapByName.get(status.getCloudResource().getGroup())));

                if (adjustmentTypeAndThreshold == null) {
                    adjustmentTypeAndThreshold = new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, (long) futures.size());
                }
                try {
                    ctx.setBuild(false);
                    CloudFailureContext cloudFailureContext = new CloudFailureContext(auth,
                            new ScaleContext(upscale, adjustmentTypeAndThreshold.getAdjustmentType(), adjustmentTypeAndThreshold.getThreshold()), ctx);
                    cloudFailureHandler.rollbackIfNecessary(cloudFailureContext, failedGroupCloudResourceStatus, groupCloudResourceStatuses,
                            getNewNodeCount(groups));
                    results.addAll(resourceStatuses);
                } catch (RuntimeException e) {
                    LOGGER.error("Failed to handle provisioning rollback", e);
                    throw e;
                } finally {
                    ctx.setBuild(true);
                }
            }
            return results;
        }

        public List<CloudResourceStatus> updateResources(ResourceBuilderContext ctx, AuthenticatedContext auth,
                List<CloudResource> computeResources, CloudStack cloudStack, Optional<String> group, UpdateType updateType) {
            List<CloudResourceStatus> results = new ArrayList<>();
            Collection<Future<List<CloudResourceStatus>>> futures = new ArrayList<>();
            Variant variant = auth.getCloudContext().getVariant();
            List<ComputeResourceBuilder<ResourceBuilderContext>> builders = resourceBuilders.compute(variant);
            int poolSize = ctx.getResourceBuilderPoolSize();
            LOGGER.info("Pool size: {} for update resources", poolSize);
            try (ExecutorService executor = new ThreadPoolExecutor(0, poolSize, 0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(), Thread.ofVirtual().factory())) {
                for (ComputeResourceBuilder<ResourceBuilderContext> builder : Lists.reverse(builders)) {
                    LOGGER.debug("Update resources: execute {} builder for the resource of {}", builder.getClass().getSimpleName(), builder.resourceType());
                    List<CloudResource> resourceList = getResources(builder.resourceType(), computeResources);
                    for (CloudResource cloudResource : resourceList) {
                        Optional<CloudInstance> instance = cloudStack.getGroups()
                                .stream()
                                .flatMap(g -> g.getInstances().stream())
                                .filter(e -> getInstanceId(cloudResource).equals(e.getInstanceId()))
                                .findFirst();
                        if (instance.isPresent()) {
                            ResourceUpdateCallablePayload resourceUpdateCallablePayload
                                    = new ResourceUpdateCallablePayload(cloudResource, instance.get(), ctx, auth, cloudStack, builder, group, updateType);
                            ResourceUpdateCallable updateCallable = resourceActionFactory.buildUpdateCallable(resourceUpdateCallablePayload);
                            Future<List<CloudResourceStatus>> future = executor.submit(updateCallable);
                            futures.add(future);
                            results.addAll(getBuilderResults(futures));
                        }
                    }
                    // wait for builder type to finish before starting the next one
                    results.addAll(getBuilderResults(futures));
                    LOGGER.debug("Update resources has been finished for builder {}", builder.getClass().getSimpleName());
                }
            }
            return results;
        }

        private String getInstanceId(CloudResource cloudResource) {
            return Strings.isNullOrEmpty(cloudResource.getInstanceId()) ? cloudResource.getName() : cloudResource.getInstanceId();
        }

        private List<CloudResourceStatus> waitForResourceCreations(List<List<CloudResourceStatus>> cloudResourceStatusChunks) {
            List<CloudResourceStatus> result = new ArrayList<>();
            for (List<CloudResourceStatus> cloudResourceStatuses : cloudResourceStatusChunks) {
                List<CloudResourceStatus> instanceResourceStatuses = cloudResourceStatuses.stream()
                        .filter(crs -> ResourceType.isInstanceResource(crs.getCloudResource().getType()))
                        .filter(crs -> ResourceStatus.IN_PROGRESS.equals(crs.getStatus())).toList();
                if (!instanceResourceStatuses.isEmpty()) {
                    LOGGER.debug("Poll {} instance's state whether they have reached the created state", instanceResourceStatuses.size());
                    CloudResource resourceProbe = instanceResourceStatuses.get(0).getCloudResource();
                    Optional<ComputeResourceBuilder<ResourceBuilderContext>> builderOpt = determineComputeResourceBuilder(resourceProbe);
                    if (builderOpt.isEmpty()) {
                        LOGGER.debug("No resource builder found for type {}", resourceProbe.getType());
                        continue;
                    }
                    ComputeResourceBuilder<ResourceBuilderContext> builder = builderOpt.get();
                    LOGGER.debug("Determined resource builder for instances: {}", builder.resourceType());
                    for (CloudResourceStatus instanceResourceStatus : instanceResourceStatuses) {
                        PollGroup pollGroup = InMemoryStateStore.getStack(auth.getCloudContext().getId());
                        if (pollGroup == null || CANCELLED.equals(pollGroup)) {
                            throw new CancellationException(format("Building of %s has been cancelled", instanceResourceStatus));
                        }
                        CloudResource instance = instanceResourceStatus.getCloudResource();
                        PollTask<List<CloudResourceStatus>> pollTask = resourcePollTaskFactory
                                .newPollResourceTask(builder, auth, List.of(instance), ctx, true);
                        try {
                            List<CloudResourceStatus> statuses = syncPollingScheduler.schedule(pollTask);
                            instanceResourceStatus.setStatus(statuses.get(0).getStatus());
                        } catch (Exception e) {
                            LOGGER.debug("Failure during polling the instance status of {}", instanceResourceStatus, e);
                            cloudResourceStatuses.stream().filter(crs -> crs.getPrivateId().equals(instanceResourceStatus.getPrivateId())).forEach(crs -> {
                                crs.setStatus(ResourceStatus.FAILED);
                                crs.setStatusReason(e.getMessage());
                            });
                        }
                    }
                    result.addAll(cloudResourceStatuses);
                } else {
                    result.addAll(cloudResourceStatuses);
                    LOGGER.debug("No instances to poll");
                }
            }
            return result;
        }

        private List<CloudResourceStatus> filterResourceStatuses(List<CloudResourceStatus> cloudResourceStatuses, ResourceStatus resourceStatus) {
            return cloudResourceStatuses.stream().filter(rs -> resourceStatus.equals(rs.getStatus())).collect(Collectors.toList());
        }

        private Optional<ComputeResourceBuilder<ResourceBuilderContext>> determineComputeResourceBuilder(CloudResource resource) {
            Variant variant = auth.getCloudContext().getVariant();
            return resourceBuilders.compute(variant)
                    .stream().filter(rb -> rb.resourceType().equals(resource.getType())).findFirst();
        }

        private Iterable<Group> getOrderedCopy(Iterable<Group> groups) {
            Ordering<Group> byLengthOrdering = new Ordering<>() {
                @Override
                public int compare(Group left, Group right) {
                    return Ints.compare(left.getInstances().size(), right.getInstances().size());
                }
            };
            return byLengthOrdering.sortedCopy(groups);
        }
    }
}
