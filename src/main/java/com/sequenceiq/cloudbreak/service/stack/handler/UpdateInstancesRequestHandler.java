package com.sequenceiq.cloudbreak.service.stack.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.connector.UserDataBuilder;
import com.sequenceiq.cloudbreak.service.stack.event.AddInstancesComplete;
import com.sequenceiq.cloudbreak.service.stack.event.StackOperationFailure;
import com.sequenceiq.cloudbreak.service.stack.event.StackUpdateSuccess;
import com.sequenceiq.cloudbreak.service.stack.event.UpdateInstancesRequest;
import com.sequenceiq.cloudbreak.service.stack.flow.FutureResult;
import com.sequenceiq.cloudbreak.service.stack.flow.ProvisionUtil;
import com.sequenceiq.cloudbreak.service.stack.flow.ResourceRequestResult;
import com.sequenceiq.cloudbreak.service.stack.handler.callable.DownScaleCallable.DownScaleCallableBuilder;
import com.sequenceiq.cloudbreak.service.stack.handler.callable.UpScaleCallable.UpScaleCallableBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.DeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.ProvisionContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderInit;

import reactor.core.Reactor;
import reactor.event.Event;
import reactor.function.Consumer;

@Component
public class UpdateInstancesRequestHandler implements Consumer<Event<UpdateInstancesRequest>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateInstancesRequestHandler.class);

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @javax.annotation.Resource
    private Map<CloudPlatform, CloudPlatformConnector> cloudPlatformConnectors;

    @javax.annotation.Resource
    private Map<CloudPlatform, List<ResourceBuilder>> instanceResourceBuilders;

    @javax.annotation.Resource
    private Map<CloudPlatform, List<ResourceBuilder>> networkResourceBuilders;

    @javax.annotation.Resource
    private Map<CloudPlatform, ResourceBuilderInit> resourceBuilderInits;

    @Autowired
    private UserDataBuilder userDataBuilder;

    @Autowired
    private Reactor reactor;

    @Autowired
    private AsyncTaskExecutor resourceBuilderExecutor;

    @Autowired
    private ProvisionUtil provisionUtil;

    @Override
    public void accept(Event<UpdateInstancesRequest> event) {
        final UpdateInstancesRequest request = event.getData();
        final Stack stack = stackRepository.findOneWithLists(request.getStackId());
        MDCBuilder.buildMdcContext(stack);
        try {
            LOGGER.info("Accepted {} event on stack.", ReactorConfig.UPDATE_INSTANCES_REQUEST_EVENT);
            stackUpdater.updateMetadataReady(stack.getId(), false);
            if (isUpScaleRequest(request)) {
                upScaleStack(request, stack);
            } else {
                downScaleStack(request, stack);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            notifyUpdateFailed(stack, e.getMessage());
        }
    }

    private boolean isUpScaleRequest(UpdateInstancesRequest request) {
        return request.getScalingAdjustment() > 0;
    }

    private void downScaleStack(UpdateInstancesRequest request, Stack stack) throws Exception {
        Set<String> instanceIds = new HashSet<>();
        int i = 0;
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            for (InstanceMetaData metadataEntry : instanceGroup.getInstanceMetaData()) {
                if (metadataEntry.isDecommissioned() || metadataEntry.isUnRegistered()) {
                    instanceIds.add(metadataEntry.getInstanceId());
                    if (++i >= request.getScalingAdjustment() * -1) {
                        break;
                    }
                }
            }
        }
        if (stack.isCloudPlatformUsedWithTemplate()) {
            cloudPlatformConnectors.get(stack.cloudPlatform()).removeInstances(stack, instanceIds, request.getInstanceGroup());
        } else {
            ResourceBuilderInit resourceBuilderInit = resourceBuilderInits.get(stack.cloudPlatform());
            final DeleteContextObject deleteContextObject = resourceBuilderInit.decommissionInit(stack, instanceIds);
            for (int j = instanceResourceBuilders.get(stack.cloudPlatform()).size() - 1; j >= 0; j--) {
                List<Future<ResourceRequestResult>> futures = new ArrayList<>();
                final int index = j;
                final ResourceBuilder resourceBuilder = instanceResourceBuilders.get(stack.cloudPlatform()).get(index);
                for (final Resource resource : getResourcesByType(resourceBuilder.resourceType(), deleteContextObject.getDecommissionResources())) {
                    Future<ResourceRequestResult> submit = resourceBuilderExecutor.submit(
                            DownScaleCallableBuilder.builder()
                                    .withStack(stack)
                                    .withDeleteContextObject(deleteContextObject)
                                    .withResource(resource)
                                    .withResourceBuilder(resourceBuilder)
                                    .build()
                    );
                    futures.add(submit);
                    if (provisionUtil.isRequestFull(stack, futures.size() + 1)) {
                        Map<FutureResult, List<ResourceRequestResult>> result = provisionUtil.waitForRequestToFinish(stack.getId(), futures);
                        provisionUtil.checkErrorOccurred(result);
                        futures = new ArrayList<>();
                    }
                }
                Map<FutureResult, List<ResourceRequestResult>> result = provisionUtil.waitForRequestToFinish(stack.getId(), futures);
                provisionUtil.checkErrorOccurred(result);
            }
            if (!stackRepository.findById(stack.getId()).isStackInDeletionPhase()) {
                stackUpdater.removeStackResources(stack.getId(), deleteContextObject.getDecommissionResources());
                LOGGER.info("Terminated instances in stack: '{}'", instanceIds);
                LOGGER.info("Publishing {} event.", ReactorConfig.REMOVE_INSTANCES_COMPLETE_EVENT);
                reactor.notify(ReactorConfig.REMOVE_INSTANCES_COMPLETE_EVENT, Event.wrap(new StackUpdateSuccess(stack.getId(), true,
                        instanceIds, request.getInstanceGroup())));
            }
        }
    }

    private void upScaleStack(UpdateInstancesRequest request, Stack stack) throws Exception {
        String userDataScript = userDataBuilder.build(stack.cloudPlatform(), stack.getHash(), stack.getConsulServers(), new HashMap<String, String>());
        if (stack.isCloudPlatformUsedWithTemplate()) {
            cloudPlatformConnectors.get(stack.cloudPlatform()).addInstances(stack, userDataScript, request.getScalingAdjustment(), request.getInstanceGroup());
        } else {
            ResourceBuilderInit resourceBuilderInit = resourceBuilderInits.get(stack.cloudPlatform());
            final ProvisionContextObject provisionContextObject = resourceBuilderInit.provisionInit(stack, userDataScript);
            for (ResourceBuilder resourceBuilder : networkResourceBuilders.get(stack.cloudPlatform())) {
                provisionContextObject.getNetworkResources().addAll(stack.getResourcesByType(resourceBuilder.resourceType()));
            }
            List<Future<ResourceRequestResult>> futures = new ArrayList<>();
            final Set<ResourceRequestResult> resourceRequestResults = new HashSet<>();
            for (int i = stack.getFullNodeCount(); i < stack.getFullNodeCount() + request.getScalingAdjustment(); i++) {
                final int index = i;
                Future<ResourceRequestResult> submit = resourceBuilderExecutor.submit(
                        UpScaleCallableBuilder.builder()
                                .withStack(stack)
                                .withStackUpdater(stackUpdater)
                                .withIndex(index)
                                .withProvisionContextObject(provisionContextObject)
                                .withInstanceResourceBuilders(instanceResourceBuilders)
                                .withInstanceGroup(request.getInstanceGroup())
                                .build()
                );
                futures.add(submit);
                if (provisionUtil.isRequestFullWithCloudPlatform(stack, futures.size() + 1)) {
                    Map<FutureResult, List<ResourceRequestResult>> result = provisionUtil.waitForRequestToFinish(stack.getId(), futures);
                    provisionUtil.checkErrorOccurred(result);
                    resourceRequestResults.addAll(result.get(FutureResult.SUCCESS));
                    futures = new ArrayList<>();
                }
            }
            Map<FutureResult, List<ResourceRequestResult>> result = provisionUtil.waitForRequestToFinish(stack.getId(), futures);
            provisionUtil.checkErrorOccurred(result);
            resourceRequestResults.addAll(result.get(FutureResult.SUCCESS));
            if (!stackRepository.findById(stack.getId()).isStackInDeletionPhase()) {
                LOGGER.info("Publishing {} event.", ReactorConfig.ADD_INSTANCES_COMPLETE_EVENT);
                reactor.notify(ReactorConfig.ADD_INSTANCES_COMPLETE_EVENT,
                        Event.wrap(new AddInstancesComplete(stack.cloudPlatform(), stack.getId(),
                                        collectResources(resourceRequestResults), request.getInstanceGroup())
                        )
                );
            }
        }
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

    private void notifyUpdateFailed(Stack stack, String detailedMessage) {
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Publishing {} event.", ReactorConfig.STACK_UPDATE_FAILED_EVENT);
        reactor.notify(ReactorConfig.STACK_UPDATE_FAILED_EVENT, Event.wrap(new StackOperationFailure(stack.getId(), detailedMessage)));
    }
}
