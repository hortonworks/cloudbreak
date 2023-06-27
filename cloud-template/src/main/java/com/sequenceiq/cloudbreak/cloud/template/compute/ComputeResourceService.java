package com.sequenceiq.cloudbreak.cloud.template.compute;

import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.CREATE_REQUESTED;
import static com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup.CANCELLED;
import static com.sequenceiq.cloudbreak.cloud.template.compute.CloudFailureHandler.ScaleContext;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
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

    @Inject
    private AsyncTaskExecutor resourceBuilderExecutor;

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
            Iterable<CloudResource> resources, boolean cancellable) {
        LOGGER.debug("Deleting the following resources: {}", resources);
        List<CloudResourceStatus> results = new ArrayList<>();
        Collection<Future<ResourceRequestResult<List<CloudResourceStatus>>>> futures = new ArrayList<>();
        Variant variant = auth.getCloudContext().getVariant();
        List<ComputeResourceBuilder<ResourceBuilderContext>> builders = Lists.reverse(resourceBuilders.compute(variant));
        for (ComputeResourceBuilder<ResourceBuilderContext> builder : builders) {
            List<CloudResource> resourceList = getResources(builder.resourceType(), resources);
            for (CloudResource cloudResource : resourceList) {
                ResourceDeletionCallable deletionCallable = resourceActionFactory.buildDeletionCallable(
                        new ResourceDeletionCallablePayload(context, auth, cloudResource, builder, cancellable));
                futures.add(resourceBuilderExecutor.submit(deletionCallable));
                if (isRequestFull(futures.size(), context)) {
                    results.addAll(getDeletedResourcesOrFail(futures));
                }
            }
            // wait for builder type to finish before starting the next one
            results.addAll(getDeletedResourcesOrFail(futures));
        }
        return results;
    }

    private List<CloudResourceStatus> getDeletedResourcesOrFail(Collection<Future<ResourceRequestResult<List<CloudResourceStatus>>>> futures) {
        Map<FutureResult, List<List<CloudResourceStatus>>> futureResults = waitForRequests(futures);
        List<List<CloudResourceStatus>> failedResources = futureResults.get(FutureResult.FAILED);
        if (!failedResources.isEmpty()) {
            throw new CloudConnectorException("Resource deletion failed. Reason: "
                    + flatList(failedResources).stream().map(CloudResourceStatus::getStatusReason).collect(Collectors.joining(" ")));
        }
        return flatList(futureResults.get(FutureResult.SUCCESS));
    }

    public List<CloudResourceStatus> update(ResourceBuilderContext ctx, AuthenticatedContext auth, CloudStack stack, List<CloudResource> cloudResource) {
        LOGGER.info("Update compute resources.");
        return new ResourceBuilder(ctx, auth).updateResources(ctx, auth, cloudResource, stack);
    }

    public List<CloudVmInstanceStatus> stopInstances(ResourceBuilderContext context, AuthenticatedContext auth, List<CloudInstance> cloudInstances) {
        return stopStart(context, auth, cloudInstances);
    }

    public List<CloudVmInstanceStatus> startInstances(ResourceBuilderContext context, AuthenticatedContext auth, List<CloudInstance> cloudInstances) {
        return stopStart(context, auth, cloudInstances);
    }

    private List<CloudVmInstanceStatus> stopStart(ResourceBuilderContext context,
            AuthenticatedContext auth, List<CloudInstance> instances) {
        List<CloudVmInstanceStatus> results = new ArrayList<>();
        Variant variant = auth.getCloudContext().getVariant();
        List<ComputeResourceBuilder<ResourceBuilderContext>> builders = resourceBuilders.compute(variant);
        if (!context.isBuild()) {
            Collections.reverse(builders);
        }

        List<ComputeResourceBuilder<ResourceBuilderContext>> instanceBuilders =
                builders.stream().filter(ComputeResourceBuilder::isInstanceBuilder).collect(Collectors.toList());

        for (ComputeResourceBuilder<ResourceBuilderContext> builder : instanceBuilders) {
            LOGGER.debug("The builder for the resource of {} is executed", builder.resourceType());

            Integer stopStartBatchSize = resourceBuilders.getStopStartBatchSize(auth.getCloudContext().getVariant());
            if (!instances.isEmpty()) {
                LOGGER.debug("Split {} instances to {} chunks to execute the stop/start operation parallel", instances.size(), stopStartBatchSize);
                AtomicInteger counter = new AtomicInteger();
                Collection<List<CloudInstance>> instancesChunks = instances.stream()
                        .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / stopStartBatchSize)).values();

                Collection<Future<ResourceRequestResult<List<CloudVmInstanceStatus>>>> futures = new ArrayList<>();
                for (List<CloudInstance> instancesChunk : instancesChunks) {
                    LOGGER.debug("Submit stop/start operation thread with {} instances", instancesChunk.size());
                    ResourceStopStartCallablePayload stopStartCallablePayload = new ResourceStopStartCallablePayload(context, auth, instancesChunk, builder);
                    ResourceStopStartCallable stopStartCallable = resourceActionFactory.buildStopStartCallable(stopStartCallablePayload);
                    Future<ResourceRequestResult<List<CloudVmInstanceStatus>>> future = resourceBuilderExecutor.submit(stopStartCallable);
                    futures.add(future);
                }

                if (!futures.isEmpty()) {
                    LOGGER.debug("Wait for all {} stop/start threads to finish", futures.size());
                    List<List<CloudVmInstanceStatus>> instancesStatuses = waitForRequests(futures).get(FutureResult.SUCCESS);
                    LOGGER.debug("There are {} success operation. Instance statuses: {}", instancesStatuses.size(), instancesStatuses);
                    List<CloudVmInstanceStatus> allVmStatuses = instancesStatuses.stream().flatMap(Collection::stream).collect(Collectors.toList());
                    List<CloudInstance> checkInstances = allVmStatuses.stream().map(CloudVmInstanceStatus::getCloudInstance).collect(Collectors.toList());
                    PollTask<List<CloudVmInstanceStatus>> pollTask = resourcePollTaskFactory.newPollComputeStatusTask(builder, auth, context, checkInstances);
                    try {
                        List<CloudVmInstanceStatus> statuses = syncVMPollingScheduler.schedule(pollTask);
                        results.addAll(statuses);
                    } catch (Exception e) {
                        LOGGER.debug("Failed to poll the instances status of {}, set the status to failed", checkInstances, e);
                        results.addAll(allVmStatuses.stream()
                                .map(vs -> new CloudVmInstanceStatus(vs.getCloudInstance(), InstanceStatus.FAILED, e.getMessage()))
                                .collect(Collectors.toList()));
                    }
                }
            } else {
                LOGGER.debug("Cloud resources are not instances so they cannot be stopped or started, skipping builder type {}", builder.resourceType());
            }
        }
        return results;
    }

    private <T> Map<FutureResult, List<T>> waitForRequests(Collection<Future<ResourceRequestResult<T>>> futures) {
        Map<FutureResult, List<T>> result = new EnumMap<>(FutureResult.class);
        result.put(FutureResult.FAILED, new ArrayList<>());
        result.put(FutureResult.SUCCESS, new ArrayList<>());
        int requests = futures.size();
        LOGGER.debug("Waiting for {} requests to finish", requests);
        try {
            for (Future<ResourceRequestResult<T>> future : futures) {
                ResourceRequestResult<T> resourceRequestResult = future.get();
                if (FutureResult.FAILED == resourceRequestResult.getStatus()) {
                    result.get(FutureResult.FAILED).add(resourceRequestResult.getResult());
                } else {
                    result.get(FutureResult.SUCCESS).add(resourceRequestResult.getResult());
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Failed to execute the request", e);
        }
        LOGGER.debug("{} requests have finished, continue with next group", requests);
        futures.clear();
        return result;
    }

    private boolean isRequestFull(int runningRequests, ResourceBuilderContext context) {
        return runningRequests % context.getParallelResourceRequest() == 0;
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

    private List<CloudResourceStatus> flatList(Iterable<List<CloudResourceStatus>> lists) {
        List<CloudResourceStatus> result = new ArrayList<>();
        for (List<CloudResourceStatus> list : lists) {
            result.addAll(list);
        }
        return result;
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

    private int getFullNodeCount(Iterable<Group> groups) {
        int fullNodeCount = 0;
        for (Group group : groups) {
            fullNodeCount += group.getInstancesSize();
        }
        return fullNodeCount;
    }

    private List<CloudResourceStatus> getBuilderResults(Collection<Future<ResourceRequestResult<List<CloudResourceStatus>>>> futures) {
        return flatList(waitForRequests(futures).get(FutureResult.SUCCESS));
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
            List<CloudResourceStatus> results = new ArrayList<>();
            Collection<Future<ResourceRequestResult<List<CloudResourceStatus>>>> futures = new ArrayList<>();
            for (Group group : getOrderedCopy(groups)) {
                List<CloudInstance> instances = group.getInstances().stream()
                        .filter(cloudInstance -> CREATE_REQUESTED.equals(cloudInstance.getTemplate().getStatus()))
                        .collect(Collectors.toList());
                Integer createBatchSize = resourceBuilders.getCreateBatchSize(auth.getCloudContext().getVariant());

                LOGGER.debug("Split the instances to {} chunks to execute the operation in parallel", createBatchSize);
                AtomicInteger counter = new AtomicInteger();
                Collection<List<CloudInstance>> instancesChunks = instances.stream()
                        .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / createBatchSize)).values();

                for (List<CloudInstance> instancesChunk : instancesChunks) {
                    LOGGER.debug("Submit the create operation thread with {} instances", instancesChunk.size());
                    ResourceCreationCallablePayload creationCallablePayload = new ResourceCreationCallablePayload(instancesChunk, group, ctx, auth, cloudStack);
                    ResourceCreationCallable creationCallable = resourceActionFactory.buildCreationCallable(creationCallablePayload);
                    Future<ResourceRequestResult<List<CloudResourceStatus>>> future = resourceBuilderExecutor.submit(creationCallable);
                    futures.add(future);
                }

                if (!futures.isEmpty()) {
                    LOGGER.debug("Wait for all {} creation threads to finish", futures.size());
                    List<List<CloudResourceStatus>> cloudResourceStatusChunks = waitForRequests(futures).get(FutureResult.SUCCESS);
                    List<CloudResourceStatus> resourceStatuses = waitForResourceCreations(cloudResourceStatusChunks);
                    List<CloudResourceStatus> failedResources = filterResourceStatuses(resourceStatuses, ResourceStatus.FAILED);
                    if (adjustmentTypeAndThreshold == null) {
                        adjustmentTypeAndThreshold = new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, (long) instances.size());
                    }
                    CloudFailureContext cloudFailureContext = new CloudFailureContext(auth,
                            new ScaleContext(upscale, adjustmentTypeAndThreshold.getAdjustmentType(), adjustmentTypeAndThreshold.getThreshold()), ctx);
                    cloudFailureHandler.rollbackIfNecessary(cloudFailureContext, failedResources, resourceStatuses, group, resourceBuilders,
                            getNewNodeCount(groups)
                    );
                    results.addAll(filterResourceStatuses(resourceStatuses, ResourceStatus.CREATED));
                }
            }
            return results;
        }

        public List<CloudResourceStatus> updateResources(ResourceBuilderContext ctx, AuthenticatedContext auth,
                List<CloudResource> computeResources, CloudStack cloudStack) {
            List<CloudResourceStatus> results = new ArrayList<>();
            Collection<Future<ResourceRequestResult<List<CloudResourceStatus>>>> futures = new ArrayList<>();
            Variant variant = auth.getCloudContext().getVariant();
            List<ComputeResourceBuilder<ResourceBuilderContext>> builders = resourceBuilders.compute(variant);
            for (ComputeResourceBuilder<ResourceBuilderContext> builder : Lists.reverse(builders)) {
                List<CloudResource> resourceList = getResources(builder.resourceType(), computeResources);
                for (CloudResource cloudResource : resourceList) {
                    Optional<CloudInstance> instance = cloudStack.getGroups()
                            .stream()
                            .flatMap(g -> g.getInstances().stream())
                            .filter(e -> getInstanceId(cloudResource).equals(e.getInstanceId()))
                            .findFirst();
                    if (instance.isPresent()) {
                        ResourceUpdateCallablePayload resourceUpdateCallablePayload
                                = new ResourceUpdateCallablePayload(cloudResource, instance.get(), ctx, auth, cloudStack, builder);
                        ResourceUpdateCallable updateCallable = resourceActionFactory.buildUpdateCallable(resourceUpdateCallablePayload);
                        Future<ResourceRequestResult<List<CloudResourceStatus>>> future = resourceBuilderExecutor.submit(updateCallable);
                        futures.add(future);
                        if (isRequestFull(futures.size(), ctx)) {
                            results.addAll(getBuilderResults(futures));
                        }
                    }
                }
                // wait for builder type to finish before starting the next one
                results.addAll(getBuilderResults(futures));
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
                        .filter(crs -> ResourceStatus.IN_PROGRESS.equals(crs.getStatus())).collect(Collectors.toList());
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
