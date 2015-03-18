package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import com.ecwid.consul.v1.ConsulClient;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.BillingStatus;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.stack.FailureHandlerService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.connector.UserDataBuilder;
import com.sequenceiq.cloudbreak.service.stack.event.StackOperationFailure;
import com.sequenceiq.cloudbreak.service.stack.event.StackUpdateSuccess;
import com.sequenceiq.cloudbreak.service.stack.handler.callable.DownScaleCallable;
import com.sequenceiq.cloudbreak.service.stack.handler.callable.UpScaleCallable;
import com.sequenceiq.cloudbreak.service.stack.resource.DeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.ProvisionContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderInit;

import reactor.core.Reactor;
import reactor.event.Event;

@Service
public class StackScalingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackScalingService.class);
    private static final int POLLING_INTERVAL = 5000;
    private static final int MAX_POLLING_ATTEMPTS = 100;

    @Autowired
    private StackService stackService;

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

    @Autowired
    @Qualifier("upscaleFailureHandlerService")
    private FailureHandlerService upscaleFailureHandlerService;

    @Autowired
    private PollingService<ConsulContext> consulPollingService;

    @Autowired
    private ConsulAgentLeaveCheckerTask consulAgentLeaveCheckerTask;

    @Autowired
    private CloudbreakEventService eventService;

    @Autowired
    private MetadataSetupContext metadataSetupContext;

    @Autowired
    private AmbariRoleAllocator ambariRoleAllocator;


    public void downscaleStack(Long stackId, String instanceGroupName, Integer scalingAdjustment) throws Exception {
        Stack stack = stackService.get(stackId);
        Set<String> instanceIds = getUnregisteredInstanceIds(scalingAdjustment, stack);
        if (stack.isCloudPlatformUsedWithTemplate()) {
            cloudPlatformConnectors.get(stack.cloudPlatform()).removeInstances(stack, instanceIds, instanceGroupName);
            //Find solution for cancellable polling problem!!!
        } else {
            removeInstancesWithResources(stack, instanceIds);
        }
        updateRemovedResourcesState(stack, instanceIds, stack.getInstanceGroupByInstanceGroupName(instanceGroupName));
        setStackAndMetadataAvailable(scalingAdjustment, stack);
    }

    public void upscaleStack(Long stackId, String instanceGroupName, Integer scalingAdjustment) throws Exception {
        Set<Resource> resources = null;
        Stack stack = stackService.get(stackId);
        String userDataScript = userDataBuilder.build(stack.cloudPlatform(), stack.getHash(), stack.getConsulServers(), new HashMap<String, String>());
        if (stack.isCloudPlatformUsedWithTemplate()) {
            cloudPlatformConnectors.get(stack.cloudPlatform()).addInstances(stack, userDataScript, scalingAdjustment, instanceGroupName);
        } else {
            resources = createInstancesWithResources(stack, userDataScript, scalingAdjustment, instanceGroupName);
        }
        InstanceGroup instanceGroup = stack.getInstanceGroupByInstanceGroupName(instanceGroupName);
        Set<CoreInstanceMetaData> coreInstanceMetaData = metadataSetupContext.updateMetadata(stack.cloudPlatform(), stack.getId(),
                resources, instanceGroupName);
        StackUpdateSuccess stackUpdateSuccess = ambariRoleAllocator.updateInstanceMetadata(stack.getId(), coreInstanceMetaData, instanceGroupName);
        int nodeCount = instanceGroup.getNodeCount() + stackUpdateSuccess.getInstanceIds().size();
        stackUpdater.updateNodeCount(stack.getId(), nodeCount, instanceGroupName);
        eventService.fireCloudbreakEvent(stack.getId(), BillingStatus.BILLING_CHANGED.name(), "Billing changed due to upscaling of cluster infrastructure.");

        setStackAndMetadataAvailable(scalingAdjustment, stack);
    }

    private Set<String> getUnregisteredInstanceIds(Integer scalingAdjustment, Stack stack) {
        Set<String> instanceIds = new HashSet<>();
        int i = 0;
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            for (InstanceMetaData metadataEntry : instanceGroup.getInstanceMetaData()) {
                if (metadataEntry.isDecommissioned() || metadataEntry.isUnRegistered()) {
                    instanceIds.add(metadataEntry.getInstanceId());
                    if (++i >= scalingAdjustment * -1) {
                        break;
                    }
                }
            }
        }
        return instanceIds;
    }

    private void removeInstancesWithResources(Stack stack, Set<String> instanceIds) throws Exception {
        ResourceBuilderInit resourceBuilderInit = resourceBuilderInits.get(stack.cloudPlatform());
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
        instanceIds.removeAll(filterFailedResources(failedResourceList, instanceIds));
        if (!stackService.get(stack.getId()).isStackInDeletionPhase()) {
            stackUpdater.removeStackResources(stack.getId(), deleteContextObject.getDecommissionResources());
            LOGGER.info("Terminated instances in stack: '{}'", instanceIds);
            LOGGER.info("Publishing {} event.", ReactorConfig.REMOVE_INSTANCES_COMPLETE_EVENT);
        }
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

    private void updateRemovedResourcesState(Stack stack, Set<String> instanceIds, InstanceGroup instanceGroup) {
        MDCBuilder.buildMdcContext(stack);
        int nodeCount = instanceGroup.getNodeCount() - instanceIds.size();
        stackUpdater.updateNodeCount(stack.getId(), nodeCount, instanceGroup.getGroupName());

        List<ConsulClient> clients = createConsulClients(stack, instanceGroup.getGroupName());
        for (InstanceMetaData instanceMetaData : instanceGroup.getInstanceMetaData()) {
            if (instanceIds.contains(instanceMetaData.getInstanceId())) {
                long timeInMillis = Calendar.getInstance().getTimeInMillis();
                instanceMetaData.setTerminationDate(timeInMillis);
                instanceMetaData.setInstanceStatus(InstanceStatus.TERMINATED);
                removeAgentFromConsul(stack, clients, instanceMetaData);
            }
        }

        stackUpdater.updateStackMetaData(stack.getId(), instanceGroup.getAllInstanceMetaData(), instanceGroup.getGroupName());
        LOGGER.info("Successfully terminated metadata of instances '{}' in stack.", instanceIds);
        eventService.fireCloudbreakEvent(stack.getId(), BillingStatus.BILLING_CHANGED.name(),
                "Billing changed due to downscaling of cluster infrastructure.");
    }

    private List<ConsulClient> createConsulClients(Stack stack, String instanceGroupName) {
        List<InstanceGroup> instanceGroups = stack.getInstanceGroupsAsList();
        List<ConsulClient> clients = Collections.emptyList();
        for (InstanceGroup instanceGroup : instanceGroups) {
            if (!instanceGroup.getGroupName().equalsIgnoreCase(instanceGroupName)) {
                clients = ConsulUtils.createClients(instanceGroup.getInstanceMetaData());
            }
        }
        return clients;
    }

    private void removeAgentFromConsul(Stack stack, List<ConsulClient> clients, InstanceMetaData metaData) {
        String nodeName = metaData.getLongName().replace(ConsulUtils.CONSUL_DOMAIN, "");
        consulPollingService.pollWithTimeout(
                consulAgentLeaveCheckerTask,
                new ConsulContext(stack, clients, Collections.singletonList(nodeName)),
                POLLING_INTERVAL,
                MAX_POLLING_ATTEMPTS);
    }

    private void setStackAndMetadataAvailable(Integer scalingAdjustment, Stack stack) {
        stackUpdater.updateMetadataReady(stack.getId(), true);
        String statusCause = String.format("%sscaling of cluster infrastructure was successful.", scalingAdjustment < 0 ? "Down" : "Up");
        stackUpdater.updateStackStatus(stack.getId(), Status.AVAILABLE, statusCause);
    }

    private Set<Resource> createInstancesWithResources(Stack stack, String userDataScript, Integer scalingAdjustment, String instanceGroup) throws Exception {
        ResourceBuilderInit resourceBuilderInit = resourceBuilderInits.get(stack.cloudPlatform());
        final ProvisionContextObject provisionContextObject = resourceBuilderInit.provisionInit(stack, userDataScript);
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
        if (!stackService.get(stack.getId()).isStackInDeletionPhase()) {
            LOGGER.info("Publishing {} event.", ReactorConfig.ADD_INSTANCES_COMPLETE_EVENT);
        }
        return collectResources(successResourceRequestResults);
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
