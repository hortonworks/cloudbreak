package com.sequenceiq.cloudbreak.service.stack.connector;

import static com.sequenceiq.cloudbreak.domain.InstanceGroupType.isGateway;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.stack.FailureHandlerService;
import com.sequenceiq.cloudbreak.service.stack.flow.FutureResult;
import com.sequenceiq.cloudbreak.service.stack.flow.ProvisionUtil;
import com.sequenceiq.cloudbreak.service.stack.flow.ResourceRequestResult;
import com.sequenceiq.cloudbreak.service.stack.flow.ScalingFailedException;
import com.sequenceiq.cloudbreak.service.stack.flow.callable.DownScaleCallable;
import com.sequenceiq.cloudbreak.service.stack.flow.callable.ProvisionContextCallable;
import com.sequenceiq.cloudbreak.service.stack.flow.callable.UpScaleCallable;
import com.sequenceiq.cloudbreak.service.stack.resource.CreateResourceRequest;
import com.sequenceiq.cloudbreak.service.stack.resource.DeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.ProvisionContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderInit;
import com.sequenceiq.cloudbreak.service.stack.resource.StartStopContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.UpdateContextObject;

@Service
public class ParallelCloudResourceManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParallelCloudResourceManager.class);

    @Inject
    private AsyncTaskExecutor resourceBuilderExecutor;
    @Inject
    private StackUpdater stackUpdater;
    @Inject
    private ProvisionUtil provisionUtil;
    @Inject
    private StackRepository stackRepository;
    @Inject
    @Qualifier("stackFailureHandlerService")
    private FailureHandlerService stackFailureHandlerService;
    @Inject
    @Qualifier("upscaleFailureHandlerService")
    private FailureHandlerService upscaleFailureHandlerService;
    @javax.annotation.Resource
    private Map<CloudPlatform, List<ResourceBuilder>> networkResourceBuilders;
    @javax.annotation.Resource
    private Map<CloudPlatform, List<ResourceBuilder>> instanceResourceBuilders;

    public Set<Resource> buildStackResources(Stack stack, String gateWayUserData, String coreUserData, ResourceBuilderInit resourceBuilderInit) {
        try {
            Map<String, String> ctxMap = MDC.getCopyOfContextMap();
            Set<Resource> resourceSet = new HashSet<>();
            CloudPlatform cloudPlatform = stack.cloudPlatform();
            final ProvisionContextObject pCO = resourceBuilderInit.provisionInit(stack);
            for (ResourceBuilder resourceBuilder : networkResourceBuilders.get(cloudPlatform)) {
                List<Resource> buildResources = resourceBuilder.buildResources(pCO, 0, Arrays.asList(resourceSet), Optional.<InstanceGroup>absent());
                CreateResourceRequest createResourceRequest = resourceBuilder.buildCreateRequest(pCO, Lists.newArrayList(resourceSet), buildResources, 0,
                        Optional.<InstanceGroup>absent(), Optional.<String>absent());
                stackUpdater.addStackResources(stack.getId(), createResourceRequest.getBuildableResources());
                resourceSet.addAll(createResourceRequest.getBuildableResources());
                pCO.getNetworkResources().addAll(createResourceRequest.getBuildableResources());
                resourceBuilder.create(createResourceRequest, stack.getRegion());
            }
            List<Future<ResourceRequestResult>> futures = new ArrayList<>();
            List<ResourceRequestResult> resourceRequestResults = new ArrayList<>();
            int fullIndex = 0;
            for (final InstanceGroup instanceGroupEntry : getOrderedCopy(stack.getInstanceGroups())) {
                for (int i = 0; i < instanceGroupEntry.getNodeCount(); i++) {
                    final int index = fullIndex;
                    final Stack finalStack = stack;
                    Future<ResourceRequestResult> submit = resourceBuilderExecutor.submit(
                            ProvisionContextCallable.ProvisionContextCallableBuilder.builder()
                                    .withIndex(index)
                                    .withInstanceGroup(instanceGroupEntry)
                                    .withInstanceResourceBuilders(instanceResourceBuilders)
                                    .withProvisionContextObject(pCO)
                                    .withStack(finalStack)
                                    .withStackUpdater(stackUpdater)
                                    .withStackRepository(stackRepository)
                                    .withUserData(isGateway(instanceGroupEntry.getInstanceGroupType()) ? gateWayUserData : coreUserData)
                                    .withMdcContextMap(ctxMap)
                                    .build()
                    );
                    futures.add(submit);
                    fullIndex++;
                    if (provisionUtil.isRequestFullWithCloudPlatform(stack, futures.size() + 1)) {
                        resourceRequestResults.addAll(provisionUtil.waitForRequestToFinish(stack.getId(), futures).get(FutureResult.FAILED));
                        stackFailureHandlerService.handleFailure(stack, resourceRequestResults);
                        futures = new ArrayList<>();
                    }
                }
            }
            resourceRequestResults.addAll(provisionUtil.waitForRequestToFinish(stack.getId(), futures).get(FutureResult.FAILED));
            stackFailureHandlerService.handleFailure(stack, resourceRequestResults);
            if (!stackRepository.findByIdLazy(stack.getId()).isStackInDeletionPhase()) {
                return resourceSet;
            } else {
                throw new CloudConnectorException("Failed to create stack resources; polling reached an invalid end state.");
            }
        } catch (Exception e) {
            LOGGER.error("Error occurred while building stack resources. Error message: {}", e.getMessage());
            throw new CloudConnectorException(e);
        }
    }

    public Set<Resource> addNewResources(Stack stack, String userDataScript, Integer scalingAdjustment, String instanceGroup,
            ResourceBuilderInit resourceBuilderInit) {
        try {
            Map<String, String> ctxMap = MDC.getCopyOfContextMap();
            final ProvisionContextObject provisionContextObject = resourceBuilderInit.provisionInit(stack);
            for (ResourceBuilder resourceBuilder : networkResourceBuilders.get(stack.cloudPlatform())) {
                provisionContextObject.getNetworkResources().addAll(stack.getResourcesByType(resourceBuilder.resourceType()));
            }
            List<Future<ResourceRequestResult>> futures = new ArrayList<>();
            final Set<ResourceRequestResult> successResourceRequestResults = new HashSet<>();
            final List<ResourceRequestResult> failedResourceRequestResults = new ArrayList<>();
            for (int i = stack.getFullNodeCount(); i < stack.getFullNodeCount() + scalingAdjustment; i++) {
                final int index = i;
                Future<ResourceRequestResult> submit = resourceBuilderExecutor.submit(
                        UpScaleCallable.UpScaleCallableBuilder.builder()
                                .withStack(stack)
                                .withStackUpdater(stackUpdater)
                                .withIndex(index)
                                .withProvisionContextObject(provisionContextObject)
                                .withInstanceResourceBuilders(instanceResourceBuilders)
                                .withInstanceGroup(instanceGroup)
                                .withUserData(userDataScript)
                                .withMdcContextMap(ctxMap)
                                .build()
                );
                futures.add(submit);
                if (provisionUtil.isRequestFullWithCloudPlatform(stack, futures.size() + 1)) {
                    Map<FutureResult, List<ResourceRequestResult>> result = provisionUtil.waitForRequestToFinish(stack.getId(), futures);
                    successResourceRequestResults.addAll(result.get(FutureResult.SUCCESS));
                    failedResourceRequestResults.addAll(result.get(FutureResult.FAILED));
                    upscaleFailureHandlerService.handleFailure(stack, failedResourceRequestResults);
                    futures = new ArrayList<>();
                }
            }
            Map<FutureResult, List<ResourceRequestResult>> result = provisionUtil.waitForRequestToFinish(stack.getId(), futures);
            successResourceRequestResults.addAll(result.get(FutureResult.SUCCESS));
            failedResourceRequestResults.addAll(result.get(FutureResult.FAILED));
            upscaleFailureHandlerService.handleFailure(stack, failedResourceRequestResults);
            if (stackRepository.findByIdLazy(stack.getId()).isStackInDeletionPhase()) {
                throw new ScalingFailedException("Upscaling of stack failed, because the stack is already in deletion phase.");
            }
            return collectResources(successResourceRequestResults);
        } catch (Exception e) {
            LOGGER.error("Error occurred when adding new resources to stack. Error message: {}", e.getMessage());
            throw new CloudConnectorException(e);
        }
    }

    public Set<String> removeExistingResources(Stack stack, Set<String> origInstanceIds, ResourceBuilderInit resourceBuilderInit) {
        try {
            Map<String, String> mdcCtxMap = MDC.getCopyOfContextMap();
            Set<String> instanceIds = new HashSet<>(origInstanceIds);
            final DeleteContextObject deleteContextObject = resourceBuilderInit.decommissionInit(stack, instanceIds);
            List<ResourceRequestResult> failedResourceList = new ArrayList<>();
            for (int j = instanceResourceBuilders.get(stack.cloudPlatform()).size() - 1; j >= 0; j--) {
                List<Future<ResourceRequestResult>> futures = new ArrayList<>();
                final int index = j;
                final ResourceBuilder resourceBuilder = instanceResourceBuilders.get(stack.cloudPlatform()).get(index);
                for (final Resource resource : getResourcesByType(resourceBuilder.resourceType(), deleteContextObject.getDecommissionResources())) {
                    Future<ResourceRequestResult> submit = resourceBuilderExecutor.submit(
                            DownScaleCallable.DownScaleCallableBuilder.builder()
                                    .withStack(stack)
                                    .withDeleteContextObject(deleteContextObject)
                                    .withResource(resource)
                                    .withResourceBuilder(resourceBuilder)
                                    .withMdcContextMap(mdcCtxMap)
                                    .build()
                    );
                    futures.add(submit);
                    if (provisionUtil.isRequestFull(stack, futures.size() + 1)) {
                        Map<FutureResult, List<ResourceRequestResult>> result = provisionUtil.waitForRequestToFinish(stack.getId(), futures);
                        failedResourceList.addAll(result.get(FutureResult.FAILED));
                        futures = new ArrayList<>();
                    }
                }
                Map<FutureResult, List<ResourceRequestResult>> result = provisionUtil.waitForRequestToFinish(stack.getId(), futures);
                failedResourceList.addAll(result.get(FutureResult.FAILED));
            }
            instanceIds = filterFailedResources(failedResourceList, instanceIds);
            if (!stackRepository.findByIdLazy(stack.getId()).isStackInDeletionPhase()) {
                stackUpdater.removeStackResources(deleteContextObject.getDecommissionResources());
                LOGGER.info("Terminated instances in stack: '{}'", instanceIds);
            } else {
                throw new ScalingFailedException("Downscaling of stack failed, because the stack is already in deletion phase.");
            }
            return instanceIds;
        } catch (Exception e) {
            LOGGER.error("Error occurred when removing existing resources from stack. Error message: {}", e.getMessage());
            throw new CloudConnectorException(e);
        }
    }

    public void terminateResources(final Stack stack, ResourceBuilderInit resourceBuilderInit) {
        try {
            final Map<String, String> mdcCtxMap = MDC.getCopyOfContextMap();
            final CloudPlatform cloudPlatform = stack.cloudPlatform();
            final DeleteContextObject dCO = resourceBuilderInit.deleteInit(stack);
            List<Future<ResourceRequestResult>> futures = new ArrayList<>();
            for (int i = instanceResourceBuilders.get(cloudPlatform).size() - 1; i >= 0; i--) {
                final int index = i;
                List<Resource> resourceByType = stack.getResourcesByType(instanceResourceBuilders.get(cloudPlatform).get(i).resourceType());
                for (final Resource resource : resourceByType) {
                    Future<ResourceRequestResult> submit = resourceBuilderExecutor.submit(new Callable<ResourceRequestResult>() {
                        @Override
                        public ResourceRequestResult call() throws Exception {
                            try {
                                MDC.setContextMap(mdcCtxMap);
                                instanceResourceBuilders.get(cloudPlatform).get(index).delete(resource, dCO, stack.getRegion());
                                stackUpdater.removeStackResources(Arrays.asList(resource));
                                return ResourceRequestResult.ResourceRequestResultBuilder.builder()
                                        .withFutureResult(FutureResult.SUCCESS)
                                        .withInstanceGroup(stack.getInstanceGroupByInstanceGroupName(resource.getInstanceGroup()))
                                        .build();
                            } catch (Exception ex) {
                                return ResourceRequestResult.ResourceRequestResultBuilder.builder()
                                        .withException(ex)
                                        .withFutureResult(FutureResult.FAILED)
                                        .withInstanceGroup(stack.getInstanceGroupByInstanceGroupName(resource.getInstanceGroup()))
                                        .build();
                            }
                        }
                    });
                    futures.add(submit);
                    if (provisionUtil.isRequestFull(stack, futures.size() + 1)) {
                        Map<FutureResult, List<ResourceRequestResult>> result = provisionUtil.waitForRequestToFinish(stack.getId(), futures);
                        checkErrorOccurred(result);
                        futures = new ArrayList<>();
                    }
                }
            }
            Map<FutureResult, List<ResourceRequestResult>> result = provisionUtil.waitForRequestToFinish(stack.getId(), futures);
            checkErrorOccurred(result);
            for (int i = networkResourceBuilders.get(cloudPlatform).size() - 1; i >= 0; i--) {
                for (Resource resource : stack.getResourcesByType(networkResourceBuilders.get(cloudPlatform).get(i).resourceType())) {
                    networkResourceBuilders.get(cloudPlatform).get(i).delete(resource, dCO, stack.getRegion());
                    stackUpdater.removeStackResources(Arrays.asList(resource));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error occurred while terminating stack resources. Error message: {}", e.getMessage());
            throw new CloudConnectorException(e);
        }
    }

    public void rollbackResources(final Stack stack, ResourceBuilderInit resourceBuilderInit) {
        try {
            final Map<String, String> mdcCtxMap = MDC.getCopyOfContextMap();
            final CloudPlatform cloudPlatform = stack.cloudPlatform();
            final DeleteContextObject dCO = resourceBuilderInit.deleteInit(stack);
            for (int i = instanceResourceBuilders.get(cloudPlatform).size() - 1; i >= 0; i--) {
                List<Future<Boolean>> futures = new ArrayList<>();
                final int index = i;
                List<Resource> resourceByType =
                        stack.getResourcesByType(instanceResourceBuilders.get(cloudPlatform).get(i).resourceType());
                for (final Resource resource : resourceByType) {
                    Future<Boolean> submit = resourceBuilderExecutor.submit(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            MDC.setContextMap(mdcCtxMap);
                            instanceResourceBuilders.get(cloudPlatform).get(index).rollback(resource, dCO, stack.getRegion());
                            stackUpdater.removeStackResources(Arrays.asList(resource));
                            return true;
                        }
                    });
                    futures.add(submit);
                }
                for (Future<Boolean> future : futures) {
                    future.get();
                }
            }
            for (int i = networkResourceBuilders.get(cloudPlatform).size() - 1; i >= 0; i--) {
                for (Resource resource
                        : stack.getResourcesByType(networkResourceBuilders.get(cloudPlatform).get(i).resourceType())) {
                    networkResourceBuilders.get(cloudPlatform).get(i).rollback(resource, dCO, stack.getRegion());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error occurred when rolling back stack resources. Error message: {}", e.getMessage());
            throw new CloudConnectorException(e);
        }
    }

    public void startStopResources(Stack stack, final boolean start, ResourceBuilderInit resourceBuilderInit) {
        boolean finished = true;
        CloudPlatform cloudPlatform = stack.cloudPlatform();
        final Map<String, String> mdcCtxMap = MDC.getCopyOfContextMap();
        try {
            final StartStopContextObject sSCO = resourceBuilderInit.startStopInit(stack);

            for (ResourceBuilder resourceBuilder : networkResourceBuilders.get(cloudPlatform)) {
                for (Resource resource : stack.getResourcesByType(resourceBuilder.resourceType())) {
                    if (start) {
                        resourceBuilder.start(sSCO, resource, stack.getRegion());
                    } else {
                        resourceBuilder.stop(sSCO, resource, stack.getRegion());
                    }
                }
            }
            List<Future<Void>> futures = new ArrayList<>();
            for (final ResourceBuilder resourceBuilder : instanceResourceBuilders.get(cloudPlatform)) {
                List<Resource> resourceByType = stack.getResourcesByType(resourceBuilder.resourceType());
                for (final Resource resource : resourceByType) {
                    final Stack finalStack = stack;
                    Future<Void> submit = resourceBuilderExecutor.submit(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            MDC.setContextMap(mdcCtxMap);
                            if (start) {
                                resourceBuilder.start(sSCO, resource, finalStack.getRegion());
                                return null;
                            } else {
                                resourceBuilder.stop(sSCO, resource, finalStack.getRegion());
                                return null;
                            }
                        }
                    });
                    futures.add(submit);
                }
            }
            processResults(futures);
        } catch (Exception e) {
            throw new CloudConnectorException(String.format("Failed to %s resources.", start ? "start" : "stop"), e);
        }
    }

    private void processResults(List<Future<Void>> futures) {
        Set<String> exceptions = new HashSet<>();
        Exception lastException = null;
        for (Future<Void> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                exceptions.add(e.getMessage());
                lastException = e;
            }
        }
        if (!exceptions.isEmpty()) {
            throw new CloudConnectorException(exceptions.toString(), lastException);
        }
    }

    public void updateAllowedSubnets(Stack stack, ResourceBuilderInit resourceBuilderInit) {
        UpdateContextObject updateContext = resourceBuilderInit.updateInit(stack);
        for (ResourceBuilder resourceBuilder : networkResourceBuilders.get(stack.cloudPlatform())) {
            resourceBuilder.update(updateContext);
        }
    }

    private List<InstanceGroup> getOrderedCopy(Set<InstanceGroup> instanceGroupSet) {
        Ordering<InstanceGroup> byLengthOrdering = new Ordering<InstanceGroup>() {
            public int compare(InstanceGroup left, InstanceGroup right) {
                return Ints.compare(left.getNodeCount(), right.getNodeCount());
            }
        };
        return byLengthOrdering.sortedCopy(instanceGroupSet);
    }

    private Set<Resource> collectResources(Set<ResourceRequestResult> resourceSet) {
        Set<Resource> resources = new HashSet<>();
        for (ResourceRequestResult resourceRequestResult : resourceSet) {
            resources.addAll(resourceRequestResult.getBuiltResources());
        }
        return resources;
    }

    private List<Resource> getResourcesByType(ResourceType resourceType, List<Resource> resources) {
        List<Resource> resourceList = new ArrayList<>();
        for (Resource resource : resources) {
            if (resourceType.equals(resource.getResourceType())) {
                resourceList.add(resource);
            }
        }
        return resourceList;
    }

    private Set<String> filterFailedResources(List<ResourceRequestResult> failedResourceList, Set<String> instanceIds) {
        Set<String> result = new HashSet<>(instanceIds);
        for (ResourceRequestResult requestResult : failedResourceList) {
            for (Resource resource : requestResult.getResources()) {
                String resourceName = resource.getResourceName();
                if (result.contains(resourceName)) {
                    result.remove(resourceName);
                }
            }
        }
        return result;
    }

    private void checkErrorOccurred(Map<FutureResult, List<ResourceRequestResult>> futureResultListMap) throws Exception {
        List<ResourceRequestResult> resourceRequestResults = futureResultListMap.get(FutureResult.FAILED);
        if (!resourceRequestResults.isEmpty()) {
            throw resourceRequestResults.get(0).getException().orNull();
        }
    }
}
