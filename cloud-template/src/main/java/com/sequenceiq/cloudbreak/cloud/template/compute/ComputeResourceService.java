package com.sequenceiq.cloudbreak.cloud.template.compute;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.template.init.ResourceBuilders;
import com.sequenceiq.cloudbreak.cloud.template.task.ResourcePollTaskFactory;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class ComputeResourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComputeResourceService.class);

    @Value("${cb.gcp.stopStart.batch.size}")
    private Integer stopStartBatchSize;

    @Value("${cb.gcp.create.batch.size}")
    private Integer createBatchSize;

    @Inject
    private AsyncTaskExecutor resourceBuilderExecutor;

    @Inject
    private ApplicationContext applicationContext;

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

    public List<CloudResourceStatus> buildResourcesForLaunch(ResourceBuilderContext ctx, AuthenticatedContext auth, CloudStack cloudStack,
            AdjustmentType adjustmentType, Long threshold) {
        return new ResourceBuilder(ctx, auth).buildResources(cloudStack, cloudStack.getGroups(), false, adjustmentType, threshold);
    }

    public List<CloudResourceStatus> buildResourcesForUpscale(ResourceBuilderContext ctx, AuthenticatedContext auth, CloudStack cloudStack,
            Iterable<Group> groups) {
        return new ResourceBuilder(ctx, auth).buildResources(cloudStack, groups, true, AdjustmentType.BEST_EFFORT, null);
    }

    public List<CloudResourceStatus> deleteResources(ResourceBuilderContext context, AuthenticatedContext auth,
            Iterable<CloudResource> resources, boolean cancellable) {
        List<CloudResourceStatus> results = new ArrayList<>();
        Collection<Future<ResourceRequestResult<List<CloudResourceStatus>>>> futures = new ArrayList<>();
        Platform platform = auth.getCloudContext().getPlatform();
        List<ComputeResourceBuilder<ResourceBuilderContext>> builders = resourceBuilders.compute(platform);
        int numberOfBuilders = builders.size();
        for (int i = numberOfBuilders - 1; i >= 0; i--) {
            ComputeResourceBuilder<?> builder = builders.get(i);
            List<CloudResource> resourceList = getResources(builder.resourceType(), resources);
            for (CloudResource cloudResource : resourceList) {
                ResourceDeleteThread thread = createThread(ResourceDeleteThread.NAME, context, auth, cloudResource, builder, cancellable);
                Future<ResourceRequestResult<List<CloudResourceStatus>>> future = resourceBuilderExecutor.submit(thread);
                futures.add(future);
                if (isRequestFull(futures.size(), context)) {
                    results.addAll(flatList(waitForRequests(futures).get(FutureResult.SUCCESS)));
                }
            }
            // wait for builder type to finish before starting the next one
            results.addAll(flatList(waitForRequests(futures).get(FutureResult.SUCCESS)));
        }
        return results;
    }

    public List<CloudVmInstanceStatus> stopInstances(ResourceBuilderContext context, AuthenticatedContext auth,
            List<CloudResource> resources, List<CloudInstance> cloudInstances) {
        return stopStart(context, auth, resources, cloudInstances);
    }

    public List<CloudVmInstanceStatus> startInstances(ResourceBuilderContext context, AuthenticatedContext auth,
            List<CloudResource> resources, List<CloudInstance> cloudInstances) {
        return stopStart(context, auth, resources, cloudInstances);
    }

    private List<CloudVmInstanceStatus> stopStart(ResourceBuilderContext context,
            AuthenticatedContext auth, List<CloudResource> resources, List<CloudInstance> instances) {
        List<CloudVmInstanceStatus> results = new ArrayList<>();
        Platform platform = auth.getCloudContext().getPlatform();
        List<ComputeResourceBuilder<ResourceBuilderContext>> builders = resourceBuilders.compute(platform);
        if (!context.isBuild()) {
            Collections.reverse(builders);
        }

        for (ComputeResourceBuilder<?> builder : builders) {
            List<CloudResource> resourceList = getResources(builder.resourceType(), resources);
            List<CloudInstance> allInstances = getCloudInstances(resourceList, instances);

            if (!allInstances.isEmpty()) {
                LOGGER.debug("Split {} instances to {} chunks to execute the stop/start operation parallel", allInstances.size(), stopStartBatchSize);
                AtomicInteger counter = new AtomicInteger();
                Collection<List<CloudInstance>> instancesChunks = allInstances.stream()
                        .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / stopStartBatchSize)).values();

                Collection<Future<ResourceRequestResult<List<CloudVmInstanceStatus>>>> futures = new ArrayList<>();
                for (List<CloudInstance> instancesChunk : instancesChunks) {
                    LOGGER.debug("Submit stop/start operation thread with {} instances", instancesChunk.size());
                    ResourceStopStartThread thread = createThread(ResourceStopStartThread.NAME, context, auth, instancesChunk, builder);
                    Future<ResourceRequestResult<List<CloudVmInstanceStatus>>> future = resourceBuilderExecutor.submit(thread);
                    futures.add(future);
                }

                if (!futures.isEmpty()) {
                    LOGGER.debug("Wait for all {} stop/start threads to finish", futures.size());
                    List<List<CloudVmInstanceStatus>> instancesStatuses = waitForRequests(futures).get(FutureResult.SUCCESS);
                    List<CloudVmInstanceStatus> allVmStatuses = instancesStatuses.stream().flatMap(Collection::stream).collect(Collectors.toList());
                    List<CloudInstance> checkInstances = allVmStatuses.stream().map(CloudVmInstanceStatus::getCloudInstance).collect(Collectors.toList());
                    PollTask<List<CloudVmInstanceStatus>> pollTask = resourcePollTaskFactory
                            .newPollComputeStatusTask(builder, auth, context, checkInstances);
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
        return isRequestFullWithCloudPlatform(1, runningRequests, context);
    }

    private boolean isRequestFullWithCloudPlatform(int numberOfBuilders, int runningRequests, ResourceBuilderContext context) {
        return (runningRequests * numberOfBuilders) % context.getParallelResourceRequest() == 0;
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

    private <T> T createThread(String name, Object... args) {
        return (T) applicationContext.getBean(name, args);
    }

    private List<CloudInstance> getCloudInstances(List<CloudResource> cloudResource, List<CloudInstance> instances) {
        List<CloudInstance> result = new ArrayList<>();
        for (CloudResource resource : cloudResource) {
            for (CloudInstance instance : instances) {
                if (instance.getInstanceId().equalsIgnoreCase(resource.getName()) || instance.getInstanceId().equalsIgnoreCase(resource.getReference())) {
                    result.add(instance);
                }
            }
        }
        return result;
    }

    private List<CloudResourceStatus> flatList(Iterable<List<CloudResourceStatus>> lists) {
        List<CloudResourceStatus> result = new ArrayList<>();
        for (List<CloudResourceStatus> list : lists) {
            result.addAll(list);
        }
        return result;
    }

    private class ResourceBuilder {

        private final ResourceBuilderContext ctx;

        private final AuthenticatedContext auth;

        ResourceBuilder(ResourceBuilderContext ctx, AuthenticatedContext auth) {
            this.ctx = ctx;
            this.auth = auth;
        }

        public List<CloudResourceStatus> buildResources(CloudStack cloudStack, Iterable<Group> groups,
                Boolean upscale, AdjustmentType adjustmentType, Long threshold) {
            List<CloudResourceStatus> results = new ArrayList<>();
            Collection<Future<ResourceRequestResult<List<CloudResourceStatus>>>> futures = new ArrayList<>();
            for (Group group : getOrderedCopy(groups)) {
                List<CloudInstance> instances = group.getInstances();

                LOGGER.debug("Split the instances to {} chunks to execute the operation in parallel", createBatchSize);
                AtomicInteger counter = new AtomicInteger();
                Collection<List<CloudInstance>> instancesChunks = instances.stream()
                        .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / createBatchSize)).values();

                for (List<CloudInstance> instancesChunk : instancesChunks) {
                    LOGGER.debug("Submit the create operation thread with {} instances", instancesChunk.size());
                    ResourceCreateThread thread = createThread(ResourceCreateThread.NAME, instancesChunk, group, ctx, auth, cloudStack);
                    Future<ResourceRequestResult<List<CloudResourceStatus>>> future = resourceBuilderExecutor.submit(thread);
                    futures.add(future);
                }

                if (!futures.isEmpty()) {
                    LOGGER.debug("Wait for all {} creation threads to finish", futures.size());
                    List<List<CloudResourceStatus>> cloudResourceStatusChunks = waitForRequests(futures).get(FutureResult.SUCCESS);
                    List<CloudResourceStatus> resourceStatuses = waitForResourceCreations(cloudResourceStatusChunks);
                    List<CloudResourceStatus> failedResources = filterResourceStatuses(resourceStatuses, ResourceStatus.FAILED);
                    cloudFailureHandler.rollback(auth, failedResources, group, getFullNodeCount(groups), ctx,
                            resourceBuilders, new ScaleContext(upscale, adjustmentType, threshold));
                    results.addAll(filterResourceStatuses(resourceStatuses, ResourceStatus.CREATED));
                }
            }
            return results;
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
            return resourceBuilders.compute(auth.getCloudContext().getPlatform())
                    .stream().filter(rb -> rb.resourceType().equals(resource.getType())).findFirst();
        }

        private int getFullNodeCount(Iterable<Group> groups) {
            int fullNodeCount = 0;
            for (Group group : groups) {
                fullNodeCount += group.getInstancesSize();
            }
            return fullNodeCount;
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
