package com.sequenceiq.cloudbreak.cloud.template.compute;

import static com.sequenceiq.cloudbreak.cloud.template.compute.CloudFailureHandler.ScaleContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
import com.sequenceiq.cloudbreak.api.model.AdjustmentType;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
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

    public List<CloudResourceStatus> buildResourcesForLaunch(ResourceBuilderContext ctx, AuthenticatedContext auth, List<Group> groups, Image image,
            AdjustmentType adjustmentType, Long threshold) {
        return buildResources(ctx, auth, groups, image, false, adjustmentType, threshold);
    }

    public List<CloudResourceStatus> buildResourcesForUpscale(ResourceBuilderContext ctx, AuthenticatedContext auth, List<Group> groups, Image image) {
        return buildResources(ctx, auth, groups, image, true, AdjustmentType.BEST_EFFORT, null);
    }

    private List<CloudResourceStatus> buildResources(ResourceBuilderContext ctx, AuthenticatedContext auth, List<Group> groups, Image image, Boolean upscale,
            AdjustmentType adjustmentType, Long threshold) {
        List<CloudResourceStatus> results = new ArrayList<>();
        int fullNodeCount = getFullNodeCount(groups);

        CloudContext cloudContext = auth.getCloudContext();
        List<Future<ResourceRequestResult<List<CloudResourceStatus>>>> futures = new ArrayList<>();
        List<ComputeResourceBuilder> builders = resourceBuilders.compute(cloudContext.getPlatform());
        for (Group group : getOrderedCopy(groups)) {
            List<CloudInstance> instances = group.getInstances();
            for (CloudInstance instance : instances) {
                ResourceCreateThread thread = createThread(ResourceCreateThread.NAME, instance.getTemplate().getPrivateId(), group, ctx, auth, image);
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

    public List<CloudResourceStatus> deleteResources(ResourceBuilderContext context, AuthenticatedContext auth,
            List<CloudResource> resources, boolean cancellable) {
        List<CloudResourceStatus> results = new ArrayList<>();
        List<Future<ResourceRequestResult<List<CloudResourceStatus>>>> futures = new ArrayList<>();
        Platform platform = auth.getCloudContext().getPlatform();
        List<ComputeResourceBuilder> builders = resourceBuilders.compute(platform);
        int numberOfBuilders = builders.size();
        for (int i = numberOfBuilders - 1; i >= 0; i--) {
            ComputeResourceBuilder builder = builders.get(i);
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

    private int getFullNodeCount(List<Group> groups) {
        int fullNodeCount = 0;
        for (Group group : groups) {
            fullNodeCount += group.getInstancesSize();
        }
        return fullNodeCount;
    }

    private List<CloudVmInstanceStatus> stopStart(ResourceBuilderContext context,
            AuthenticatedContext auth, List<CloudResource> resources, List<CloudInstance> instances) {
        List<CloudVmInstanceStatus> results = new ArrayList<>();
        List<Future<ResourceRequestResult<List<CloudVmInstanceStatus>>>> futures = new ArrayList<>();
        Platform platform = auth.getCloudContext().getPlatform();
        List<ComputeResourceBuilder> builders = resourceBuilders.compute(platform);
        if (!context.isBuild()) {
            Collections.reverse(builders);
        }
        for (ComputeResourceBuilder builder : builders) {
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

    private <T> Map<FutureResult, List<T>> waitForRequests(List<Future<ResourceRequestResult<T>>> futures) {
        Map<FutureResult, List<T>> result = new HashMap<>();
        result.put(FutureResult.FAILED, new ArrayList<>());
        result.put(FutureResult.SUCCESS, new ArrayList<>());
        int requests = futures.size();
        LOGGER.info("Waiting for {} requests to finish", requests);
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
        LOGGER.info("{} requests have finished, continue with next group", requests);
        futures.clear();
        return result;
    }

    private boolean isRequestFull(int runningRequests, ResourceBuilderContext context) {
        return isRequestFullWithCloudPlatform(1, runningRequests, context);
    }

    private boolean isRequestFullWithCloudPlatform(int numberOfBuilders, int runningRequests, ResourceBuilderContext context) {
        return (runningRequests * numberOfBuilders) % context.getParallelResourceRequest() == 0;
    }

    private List<CloudResource> getResources(ResourceType resourceType, List<CloudResource> resources) {
        List<CloudResource> selected = new ArrayList<>();
        for (CloudResource resource : resources) {
            if (resourceType == resource.getType()) {
                selected.add(resource);
            }
        }
        return selected;
    }

    private List<Group> getOrderedCopy(List<Group> groups) {
        Ordering<Group> byLengthOrdering = new Ordering<Group>() {
            public int compare(Group left, Group right) {
                return Ints.compare(left.getInstances().size(), right.getInstances().size());
            }
        };
        return byLengthOrdering.sortedCopy(groups);
    }

    private <T> T createThread(String name, Object... args) {
        return (T) applicationContext.getBean(name, args);
    }

    private CloudInstance getCloudInstance(CloudResource cloudResource, List<CloudInstance> instances) {
        for (CloudInstance instance : instances) {
            if (instance.getInstanceId().equalsIgnoreCase(cloudResource.getName()) || instance.getInstanceId().equalsIgnoreCase(cloudResource.getReference())) {
                return instance;
            }
        }
        return null;
    }

    private List<CloudVmInstanceStatus> flatVmList(List<List<CloudVmInstanceStatus>> lists) {
        List<CloudVmInstanceStatus> result = new ArrayList<>();
        for (List<CloudVmInstanceStatus> list : lists) {
            result.addAll(list);
        }
        return result;
    }

    private List<CloudResourceStatus> flatList(List<List<CloudResourceStatus>> lists) {
        List<CloudResourceStatus> result = new ArrayList<>();
        for (List<CloudResourceStatus> list : lists) {
            result.addAll(list);
        }
        return result;
    }

}
