package com.sequenceiq.cloudbreak.cloud.template.compute;

import static com.sequenceiq.cloudbreak.cloud.template.compute.CloudFailureHandler.ScaleContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.AdjustmentType;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.template.init.ResourceBuilders;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@Service
public class ComputeResourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComputeResourceService.class);

    @Inject
    private AsyncTaskExecutor resourceBuilderExecutor;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private ResourceBuilders resourceBuilders;

    @Inject
    private CloudFailureHandler cloudFailureHandler;

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
            Iterable<CloudResource> resources, Iterable<CloudInstance> cloudInstances) {
        return stopStart(context, auth, resources, cloudInstances);
    }

    public List<CloudVmInstanceStatus> startInstances(ResourceBuilderContext context, AuthenticatedContext auth,
            Iterable<CloudResource> resources, Iterable<CloudInstance> cloudInstances) {
        return stopStart(context, auth, resources, cloudInstances);
    }

    private List<CloudVmInstanceStatus> stopStart(ResourceBuilderContext context,
            AuthenticatedContext auth, Iterable<CloudResource> resources, Iterable<CloudInstance> instances) {
        List<CloudVmInstanceStatus> results = new ArrayList<>();
        Collection<Future<ResourceRequestResult<List<CloudVmInstanceStatus>>>> futures = new ArrayList<>();
        Platform platform = auth.getCloudContext().getPlatform();
        List<ComputeResourceBuilder<ResourceBuilderContext>> builders = resourceBuilders.compute(platform);
        if (!context.isBuild()) {
            Collections.reverse(builders);
        }
        for (ComputeResourceBuilder<?> builder : builders) {
            List<CloudResource> resourceList = getResources(builder.resourceType(), resources);
            for (CloudResource cloudResource : resourceList) {
                CloudInstance instance = getCloudInstance(cloudResource, instances);
                if (instance != null) {
                    ResourceStopStartThread thread = createThread(ResourceStopStartThread.NAME, context, auth, cloudResource, instance, builder);
                    Future<ResourceRequestResult<List<CloudVmInstanceStatus>>> future = resourceBuilderExecutor.submit(thread);
                    futures.add(future);
                    if (isRequestFull(futures.size(), context)) {
                        results.addAll(flatVmList(waitForRequests(futures).get(FutureResult.SUCCESS)));
                    }
                } else {
                    break;
                }
            }
        }
        results.addAll(flatVmList(waitForRequests(futures).get(FutureResult.SUCCESS)));
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

    private CloudInstance getCloudInstance(CloudResource cloudResource, Iterable<CloudInstance> instances) {
        for (CloudInstance instance : instances) {
            if (instance.getInstanceId().equalsIgnoreCase(cloudResource.getName()) || instance.getInstanceId().equalsIgnoreCase(cloudResource.getReference())) {
                return instance;
            }
        }
        return null;
    }

    private Collection<CloudVmInstanceStatus> flatVmList(Iterable<List<CloudVmInstanceStatus>> lists) {
        Collection<CloudVmInstanceStatus> result = new ArrayList<>();
        for (List<CloudVmInstanceStatus> list : lists) {
            result.addAll(list);
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

        public List<CloudResourceStatus> buildResources(CloudStack cloudStack, Iterable<Group> groups, Boolean upscale, AdjustmentType adjustmentType,
                Long threshold) {
            List<CloudResourceStatus> results = new ArrayList<>();
            int fullNodeCount = getFullNodeCount(groups);

            CloudContext cloudContext = auth.getCloudContext();
            Collection<Future<ResourceRequestResult<List<CloudResourceStatus>>>> futures = new ArrayList<>();
            List<ComputeResourceBuilder<ResourceBuilderContext>> builders = resourceBuilders.compute(cloudContext.getPlatform());
            for (Group group : getOrderedCopy(groups)) {
                List<CloudInstance> instances = group.getInstances();
                for (CloudInstance instance : instances) {
                    ResourceCreateThread thread = createThread(ResourceCreateThread.NAME, instance.getTemplate().getPrivateId(), group, ctx, auth, cloudStack);
                    Future<ResourceRequestResult<List<CloudResourceStatus>>> future = resourceBuilderExecutor.submit(thread);
                    futures.add(future);
                    if (isRequestFullWithCloudPlatform(builders.size(), futures.size(), ctx)) {
                        Map<FutureResult, List<List<CloudResourceStatus>>> futureResultListMap = waitForRequests(futures);
                        results.addAll(flatList(futureResultListMap.get(FutureResult.SUCCESS)));
                        results.addAll(flatList(futureResultListMap.get(FutureResult.FAILED)));
                        cloudFailureHandler.rollback(auth, flatList(futureResultListMap.get(FutureResult.FAILED)), group,
                                fullNodeCount, ctx, resourceBuilders, new ScaleContext(upscale, adjustmentType, threshold));
                    }
                }
                Map<FutureResult, List<List<CloudResourceStatus>>> futureResultListMap = waitForRequests(futures);
                results.addAll(flatList(futureResultListMap.get(FutureResult.SUCCESS)));
                results.addAll(flatList(futureResultListMap.get(FutureResult.FAILED)));
                cloudFailureHandler.rollback(auth, flatList(futureResultListMap.get(FutureResult.FAILED)), group, fullNodeCount, ctx,
                        resourceBuilders, new ScaleContext(upscale, adjustmentType, threshold));
            }
            return results;
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
