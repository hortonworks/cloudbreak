package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.controller.BuildStackFailureException;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.WebsocketEndPoint;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.connector.Provisioner;
import com.sequenceiq.cloudbreak.service.stack.connector.UserDataBuilder;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionComplete;
import com.sequenceiq.cloudbreak.service.stack.event.StackOperationFailure;
import com.sequenceiq.cloudbreak.service.stack.resource.ProvisionContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderInit;
import com.sequenceiq.cloudbreak.websocket.WebsocketService;
import com.sequenceiq.cloudbreak.websocket.message.StatusMessage;

import reactor.core.Reactor;
import reactor.event.Event;

@Service
public class ProvisionContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisionContext.class);

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Autowired
    private WebsocketService websocketService;

    @javax.annotation.Resource
    private Map<CloudPlatform, Provisioner> provisioners;

    @Autowired
    private Reactor reactor;

    @Autowired
    private UserDataBuilder userDataBuilder;

    @javax.annotation.Resource
    private Map<CloudPlatform, List<ResourceBuilder>> instanceResourceBuilders;

    @javax.annotation.Resource
    private Map<CloudPlatform, List<ResourceBuilder>> networkResourceBuilders;

    @javax.annotation.Resource
    private ConcurrentTaskExecutor resourceBuilderExecutor;

    @javax.annotation.Resource
    private Map<CloudPlatform, ResourceBuilderInit> resourceBuilderInits;

    public void buildStack(final CloudPlatform cloudPlatform, Long stackId, Map<String, Object> setupProperties, Map<String, String> userDataParams) {
        try {
            Stack stack = stackRepository.findById(stackId);
            if (stack.getStatus().equals(Status.REQUESTED)) {
                stack = stackUpdater.updateStackStatus(stack.getId(), Status.CREATE_IN_PROGRESS);
                websocketService.sendToTopicUser(stack.getOwner(), WebsocketEndPoint.STACK, new StatusMessage(stack.getId(), stack.getName(), stack
                        .getStatus().name()));
                stackUpdater.updateStackStatusReason(stack.getId(), stack.getStatus().name());
                if (!cloudPlatform.isWithTemplate()) {
                    stackUpdater.updateStackStatus(stack.getId(), Status.REQUESTED);
                    Set<com.sequenceiq.cloudbreak.domain.Resource> resourceSet = new HashSet<>();
                    ResourceBuilderInit resourceBuilderInit = resourceBuilderInits.get(cloudPlatform);
                    final ProvisionContextObject pCO =
                            resourceBuilderInit.provisionInit(stack, userDataBuilder.build(cloudPlatform, stack.getHash(), userDataParams));
                    for (ResourceBuilder resourceBuilder : networkResourceBuilders.get(cloudPlatform)) {
                        List<Resource> resourceList = resourceBuilder.create(pCO);
                        resourceSet.addAll(resourceList);
                        pCO.getNetworkResources().addAll(resourceList);
                    }
                    List<Future<List<Resource>>> futures = new ArrayList<>();
                    for (int i = 0; i < stack.getNodeCount(); i++) {
                        final int index = i;
                        Future<List<Resource>> submit = resourceBuilderExecutor.submit(new Callable<List<Resource>>() {
                            @Override
                            public List<Resource> call() throws Exception {
                                List<Resource> resources = new ArrayList<>();
                                for (final ResourceBuilder resourceBuilder : instanceResourceBuilders.get(cloudPlatform)) {
                                    List<Resource> resourceList = resourceBuilder.create(pCO, index, resources);
                                    resources.addAll(resourceList);
                                }
                                return resources;
                            }
                        });
                        futures.add(submit);
                    }
                    for (Future<List<Resource>> future : futures) {
                        try {
                            resourceSet.addAll(future.get());
                        } catch (Exception e) {
                            throw new BuildStackFailureException(e.getMessage(), e, resourceSet);
                        }
                    }

                    LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.PROVISION_COMPLETE_EVENT, stack.getId());
                    reactor.notify(ReactorConfig.PROVISION_COMPLETE_EVENT, Event.wrap(new ProvisionComplete(CloudPlatform.GCC, stack.getId(), resourceSet)));
                } else {
                    Provisioner provisioner = provisioners.get(cloudPlatform);
                    provisioner.buildStack(stack, userDataBuilder.build(cloudPlatform, stack.getHash(), userDataParams), setupProperties);
                }
            } else {
                LOGGER.info("CloudFormation stack creation was requested for a stack, that is not in REQUESTED status anymore. [stackId: '{}', status: '{}']",
                        stack.getId(), stack.getStatus());
            }
        } catch (BuildStackFailureException e) {
            stackUpdater.updateStackResources(stackId, e.getResourceSet());
            LOGGER.error("Unhandled exception occured while creating stack.", e);
            LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.STACK_CREATE_FAILED_EVENT, stackId);
            StackOperationFailure stackCreationFailure = new StackOperationFailure(stackId, "Internal server error occured while creating stack.");
            reactor.notify(ReactorConfig.STACK_CREATE_FAILED_EVENT, Event.wrap(stackCreationFailure));
        } catch (Exception e) {
            LOGGER.error("Unhandled exception occured while creating stack.", e);
            LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.STACK_CREATE_FAILED_EVENT, stackId);
            StackOperationFailure stackCreationFailure = new StackOperationFailure(stackId, "Internal server error occured while creating stack.");
            reactor.notify(ReactorConfig.STACK_CREATE_FAILED_EVENT, Event.wrap(stackCreationFailure));
        }
    }

}
