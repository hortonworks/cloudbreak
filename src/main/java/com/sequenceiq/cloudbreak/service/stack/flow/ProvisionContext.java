package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.ArrayList;
import java.util.Arrays;
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

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.FailureHandlerService;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.connector.UserDataBuilder;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionComplete;
import com.sequenceiq.cloudbreak.service.stack.flow.callable.ProvisionContextCallable.ProvisionContextCallableBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.CreateResourceRequest;
import com.sequenceiq.cloudbreak.service.stack.resource.ProvisionContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderInit;

@Service
public class ProvisionContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisionContext.class);

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @javax.annotation.Resource
    private Map<CloudPlatform, CloudPlatformConnector> cloudPlatformConnectors;

    @Autowired
    private UserDataBuilder userDataBuilder;

    @javax.annotation.Resource
    private Map<CloudPlatform, List<ResourceBuilder>> instanceResourceBuilders;

    @javax.annotation.Resource
    private Map<CloudPlatform, List<ResourceBuilder>> networkResourceBuilders;

    @Autowired
    private AsyncTaskExecutor resourceBuilderExecutor;

    @javax.annotation.Resource
    private Map<CloudPlatform, ResourceBuilderInit> resourceBuilderInits;

    @Autowired
    private ProvisionUtil provisionUtil;

    @Autowired
    @Qualifier("stackFailureHandlerService")
    private FailureHandlerService stackFailureHandlerService;

    public ProvisionComplete buildStack(final CloudPlatform cloudPlatform, Long stackId, Map<String, Object> setupProperties,
            Map<String, String> userDataParams) throws Exception {
        ProvisionComplete provisionComplete = null;

        Stack stack = stackRepository.findOneWithLists(stackId);
        MDCBuilder.buildMdcContext(stack);
        if (stack.getStatus().equals(Status.REQUESTED)) {
            String statusReason = "Creation of cluster infrastructure has started on the cloud provider.";
            stack = stackUpdater.updateStackStatus(stack.getId(), Status.CREATE_IN_PROGRESS, statusReason);
            stackUpdater.updateStackStatusReason(stack.getId(), stack.getStatus().name());
            String userDataScript = userDataBuilder.build(cloudPlatform, stack.getHash(), stack.getConsulServers(), userDataParams);
            if (!cloudPlatform.isWithTemplate()) {
                stackUpdater.updateStackStatus(stack.getId(), Status.REQUESTED, "Creation of cluster infrastructure has been requested.");
                Set<Resource> resourceSet = new HashSet<>();
                ResourceBuilderInit resourceBuilderInit = resourceBuilderInits.get(cloudPlatform);
                final ProvisionContextObject pCO = resourceBuilderInit.provisionInit(stack, userDataScript);
                for (ResourceBuilder resourceBuilder : networkResourceBuilders.get(cloudPlatform)) {
                    List<Resource> buildResources = resourceBuilder.buildResources(pCO, 0, Arrays.asList(resourceSet), Optional.<InstanceGroup>absent());
                    CreateResourceRequest createResourceRequest =
                            resourceBuilder.buildCreateRequest(pCO, Lists.newArrayList(resourceSet), buildResources, 0, Optional.<InstanceGroup>absent());
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
                                ProvisionContextCallableBuilder.builder()
                                        .withIndex(index)
                                        .withInstanceGroup(instanceGroupEntry)
                                        .withInstanceResourceBuilders(instanceResourceBuilders)
                                        .withProvisionContextObject(pCO)
                                        .withStack(finalStack)
                                        .withStackUpdater(stackUpdater)
                                        .withStackRepository(stackRepository)
                                        .build()
                        );
                        futures.add(submit);
                        fullIndex++;
                        if (provisionUtil.isRequestFullWithCloudPlatform(stack, futures.size() + 1)) {
                            resourceRequestResults.addAll(provisionUtil.waitForRequestToFinish(stackId, futures).get(FutureResult.FAILED));
                            stackFailureHandlerService.handleFailure(stack, resourceRequestResults);
                            futures = new ArrayList<>();
                        }
                    }
                }
                resourceRequestResults.addAll(provisionUtil.waitForRequestToFinish(stackId, futures).get(FutureResult.FAILED));
                stackFailureHandlerService.handleFailure(stack, resourceRequestResults);
                if (!stackRepository.findById(stackId).isStackInDeletionPhase()) {
                    LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.PROVISION_COMPLETE_EVENT, stack.getId());
                    provisionComplete = new ProvisionComplete(cloudPlatform, stack.getId(), resourceSet);
                }
            } else {
                CloudPlatformConnector cloudPlatformConnector = cloudPlatformConnectors.get(cloudPlatform);
                Set<Resource> resources = cloudPlatformConnector.buildStack(stack, userDataScript, setupProperties);
                provisionComplete = new ProvisionComplete(cloudPlatform, stack.getId(), resources);
            }
        } else {
            LOGGER.info("CloudFormation stack creation was requested for a stack, that is not in REQUESTED status anymore. [stackId: '{}', status: '{}']",
                    stack.getId(), stack.getStatus());
        }
        return provisionComplete;
    }

    private List<InstanceGroup> getOrderedCopy(Set<InstanceGroup> instanceGroupSet) {
        Ordering<InstanceGroup> byLengthOrdering = new Ordering<InstanceGroup>() {
            public int compare(InstanceGroup left, InstanceGroup right) {
                return Ints.compare(left.getNodeCount(), right.getNodeCount());
            }
        };
        return byLengthOrdering.sortedCopy(instanceGroupSet);
    }

}
