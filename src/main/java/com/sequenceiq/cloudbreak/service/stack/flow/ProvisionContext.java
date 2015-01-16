package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.controller.BuildStackFailureException;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.connector.UserDataBuilder;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionComplete;
import com.sequenceiq.cloudbreak.service.stack.event.StackOperationFailure;
import com.sequenceiq.cloudbreak.service.stack.resource.CreateResourceRequest;
import com.sequenceiq.cloudbreak.service.stack.resource.ProvisionContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderInit;

import reactor.core.Reactor;
import reactor.event.Event;

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
    private Reactor reactor;

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

    public void buildStack(final CloudPlatform cloudPlatform, Long stackId, Map<String, Object> setupProperties, Map<String, String> userDataParams) {
        Stack stack = stackRepository.findOneWithLists(stackId);
        MDCBuilder.buildMdcContext(stack);
        try {
            if (stack.getStatus().equals(Status.REQUESTED)) {
                String statusReason = "Creation of cluster infrastructure has started on the cloud provider.";
                stack = stackUpdater.updateStackStatus(stack.getId(), Status.CREATE_IN_PROGRESS, statusReason);
                stackUpdater.updateStackStatusReason(stack.getId(), stack.getStatus().name());
                if (!cloudPlatform.isWithTemplate()) {
                    stackUpdater.updateStackStatus(stack.getId(), Status.REQUESTED, "Creation of cluster infrastructure has been requested.");
                    Set<Resource> resourceSet = new HashSet<>();
                    ResourceBuilderInit resourceBuilderInit = resourceBuilderInits.get(cloudPlatform);
                    final ProvisionContextObject pCO =
                            resourceBuilderInit.provisionInit(stack, userDataBuilder.build(cloudPlatform, stack.getHash(), userDataParams));
                    for (ResourceBuilder resourceBuilder : networkResourceBuilders.get(cloudPlatform)) {
                        CreateResourceRequest createResourceRequest =
                                resourceBuilder.buildCreateRequest(pCO,
                                        Lists.newArrayList(resourceSet),
                                        resourceBuilder.buildResources(pCO, 0, Arrays.asList(resourceSet),
                                                Lists.newArrayList(stack.getInstanceGroups()).get(0)),
                                        0,
                                        Lists.newArrayList(stack.getInstanceGroups()).get(0));
                        stackUpdater.addStackResources(stack.getId(), createResourceRequest.getBuildableResources());
                        resourceSet.addAll(createResourceRequest.getBuildableResources());
                        pCO.getNetworkResources().addAll(createResourceRequest.getBuildableResources());
                        List<InstanceGroup> instanceGroups = Lists.newArrayList(stack.getInstanceGroups());
                        resourceBuilder.create(createResourceRequest, instanceGroups.get(0), stack.getRegion());
                    }
                    List<Future<Boolean>> futures = new ArrayList<>();
                    int fullIndex = 0;
                    for (final InstanceGroup stringInstanceGroupEntry : stack.getInstanceGroups()) {
                        for (int i = 0; i < stringInstanceGroupEntry.getNodeCount(); i++) {
                            final int index = fullIndex;
                            final Stack finalStack = stack;
                            Future<Boolean> submit = resourceBuilderExecutor.submit(new Callable<Boolean>() {
                                @Override
                                public Boolean call() throws Exception {
                                    LOGGER.info("Node {}. creation starting", index);
                                    List<Resource> resources = new ArrayList<>();
                                    for (final ResourceBuilder resourceBuilder : instanceResourceBuilders.get(cloudPlatform)) {
                                        CreateResourceRequest createResourceRequest =
                                                resourceBuilder.buildCreateRequest(pCO,
                                                        resources,
                                                        resourceBuilder.buildResources(pCO, index, resources, stringInstanceGroupEntry),
                                                        index,
                                                        stringInstanceGroupEntry);
                                        stackUpdater.addStackResources(finalStack.getId(), createResourceRequest.getBuildableResources());
                                        resourceBuilder.create(createResourceRequest, stringInstanceGroupEntry, finalStack.getRegion());
                                        resources.addAll(createResourceRequest.getBuildableResources());
                                        LOGGER.info("Node {}. creation in progress resource {} creation finished.", index,
                                                resourceBuilder.resourceBuilderType());
                                    }
                                    return true;
                                }
                            });
                            futures.add(submit);
                            fullIndex++;
                        }
                    }

                    StringBuilder sb = new StringBuilder();
                    Optional<Exception> exception = Optional.absent();
                    for (Future<Boolean> future : futures) {
                        try {
                            future.get();
                        } catch (Exception ex) {
                            exception = Optional.fromNullable(ex);
                            sb.append(String.format("%s, ", ex.getMessage()));
                        }
                    }
                    if (exception.isPresent()) {
                        throw new BuildStackFailureException(sb.toString(), exception.orNull(), stackRepository.findOneWithLists(stackId).getResources());
                    }

                    LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.PROVISION_COMPLETE_EVENT, stack.getId());
                    reactor.notify(ReactorConfig.PROVISION_COMPLETE_EVENT, Event.wrap(new ProvisionComplete(cloudPlatform, stack.getId(), resourceSet)));
                } else {
                    CloudPlatformConnector cloudPlatformConnector = cloudPlatformConnectors.get(cloudPlatform);
                    cloudPlatformConnector.buildStack(stack, userDataBuilder.build(cloudPlatform, stack.getHash(), userDataParams), setupProperties);
                }
            } else {
                LOGGER.info("CloudFormation stack creation was requested for a stack, that is not in REQUESTED status anymore. [stackId: '{}', status: '{}']",
                        stack.getId(), stack.getStatus());
            }
        } catch (BuildStackFailureException e) {
            LOGGER.error("Unhandled exception occured while creating stack.", e);
            LOGGER.info("Publishing {} event.", ReactorConfig.STACK_CREATE_FAILED_EVENT);
            StackOperationFailure stackCreationFailure = new StackOperationFailure(stackId, "Internal server error occured while creating stack.");
            reactor.notify(ReactorConfig.STACK_CREATE_FAILED_EVENT, Event.wrap(stackCreationFailure));
        } catch (Exception e) {
            LOGGER.error("Unhandled exception occured while creating stack.", e);
            LOGGER.info("Publishing {} event.", ReactorConfig.STACK_CREATE_FAILED_EVENT);
            StackOperationFailure stackCreationFailure = new StackOperationFailure(stackId, "Internal server error occured while creating stack.");
            reactor.notify(ReactorConfig.STACK_CREATE_FAILED_EVENT, Event.wrap(stackCreationFailure));
        }
    }

}
