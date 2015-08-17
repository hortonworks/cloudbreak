package com.sequenceiq.cloudbreak.cloud.template.compute;

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
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.init.ResourceBuilders;
import com.sequenceiq.cloudbreak.domain.ResourceType;

@Service
public class ComputeResourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComputeResourceService.class);

    @Inject
    private AsyncTaskExecutor resourceBuilderExecutor;
    @Inject
    private ApplicationContext applicationContext;
    @Inject
    private ResourceBuilders resourceBuilders;

    public List<CloudResourceStatus> buildResources(ResourceBuilderContext ctx, AuthenticatedContext auth, List<Group> groups, Image image) throws Exception {
        List<CloudResourceStatus> results = new ArrayList<>();
        CloudContext cloudContext = auth.getCloudContext();
        List<Future<ResourceRequestResult<List<CloudResourceStatus>>>> futures = new ArrayList<>();
        List<ComputeResourceBuilder> builders = resourceBuilders.compute(cloudContext.getPlatform());
        for (Group group : getOrderedCopy(groups)) {
            List<InstanceTemplate> instances = group.getInstances();
            for (int i = 0; i < instances.size(); i++) {
                ResourceCreateThread thread = createThread(ResourceCreateThread.NAME, instances.get(i).getPrivateId(), group, ctx, auth, image);
                Future<ResourceRequestResult<List<CloudResourceStatus>>> future = resourceBuilderExecutor.submit(thread);
                futures.add(future);
                if (isRequestFullWithCloudPlatform(builders.size(), futures.size(), ctx)) {
                    results.addAll(flatList(waitForRequests(futures).get(FutureResult.SUCCESS)));
                    //TODO rollback failed resources and decrease node count
                }
            }
        }
        results.addAll(flatList(waitForRequests(futures).get(FutureResult.SUCCESS)));
        return results;
    }

    public List<CloudResourceStatus> deleteResources(ResourceBuilderContext context, AuthenticatedContext auth,
            List<CloudResource> resources, boolean cancellable) throws Exception {
        List<CloudResourceStatus> results = new ArrayList<>();
        List<Future<ResourceRequestResult<List<CloudResourceStatus>>>> futures = new ArrayList<>();
        String platform = auth.getCloudContext().getPlatform();
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
                    //TODO rollback failed resources and decrease node count
                }
            }
            // wait for builder type to finish before starting the next one
            results.addAll(flatList(waitForRequests(futures).get(FutureResult.SUCCESS)));
        }
        return results;
    }

    public List<CloudVmInstanceStatus> stopInstances(ResourceBuilderContext context, AuthenticatedContext auth,
            List<CloudResource> resources, List<CloudInstance> cloudInstances) throws Exception {
        return stopStart(context, auth, resources, cloudInstances);
    }

    public List<CloudVmInstanceStatus> startInstances(ResourceBuilderContext context, AuthenticatedContext auth,
            List<CloudResource> resources, List<CloudInstance> cloudInstances) throws Exception {
        return stopStart(context, auth, resources, cloudInstances);
    }

    private List<CloudVmInstanceStatus> stopStart(ResourceBuilderContext context,
            AuthenticatedContext auth, List<CloudResource> resources, List<CloudInstance> instances) throws Exception {
        List<CloudVmInstanceStatus> results = new ArrayList<>();
        List<Future<ResourceRequestResult<List<CloudVmInstanceStatus>>>> futures = new ArrayList<>();
        String platform = auth.getCloudContext().getPlatform();
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

    private <T> Map<FutureResult, List<T>> waitForRequests(List<Future<ResourceRequestResult<T>>> futures) throws Exception {
        Map<FutureResult, List<T>> result = new HashMap<>();
        result.put(FutureResult.FAILED, new ArrayList<T>());
        result.put(FutureResult.SUCCESS, new ArrayList<T>());
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
            if (e instanceof CancellationException) {
                throw e;
            } else {
                LOGGER.error("Failed to execute the request", e);
            }
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
            if (instance.getInstanceId().equalsIgnoreCase(cloudResource.getName())) {
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
