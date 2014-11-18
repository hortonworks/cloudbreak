package com.sequenceiq.cloudbreak.service.stack.handler;

import java.util.ArrayList;
import java.util.HashMap;
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
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.controller.BuildStackFailureException;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.AddInstancesFailedException;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.connector.UserDataBuilder;
import com.sequenceiq.cloudbreak.service.stack.event.AddInstancesComplete;
import com.sequenceiq.cloudbreak.service.stack.event.StackOperationFailure;
import com.sequenceiq.cloudbreak.service.stack.event.StackUpdateSuccess;
import com.sequenceiq.cloudbreak.service.stack.event.UpdateInstancesRequest;
import com.sequenceiq.cloudbreak.service.stack.resource.DeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.ProvisionContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderInit;

import groovyx.net.http.HttpResponseException;
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

    @Override
    public void accept(Event<UpdateInstancesRequest> event) {
        UpdateInstancesRequest request = event.getData();
        final CloudPlatform cloudPlatform = request.getCloudPlatform();
        Long stackId = request.getStackId();
        Integer scalingAdjustment = request.getScalingAdjustment();
        final Stack stack = stackRepository.findOneWithLists(stackId);
        MDCBuilder.buildMdcContext(stack);
        try {
            LOGGER.info("Accepted {} event on stack.", ReactorConfig.UPDATE_INSTANCES_REQUEST_EVENT);
            stackUpdater.updateMetadataReady(stackId, false);
            if (scalingAdjustment > 0) {
                if (cloudPlatform.isWithTemplate()) {
                    cloudPlatformConnectors.get(cloudPlatform)
                            .addInstances(stack, userDataBuilder.build(cloudPlatform, stack.getHash(), new HashMap<String, String>()), scalingAdjustment);
                } else {
                    ResourceBuilderInit resourceBuilderInit = resourceBuilderInits.get(cloudPlatform);
                    final ProvisionContextObject pCO =
                            resourceBuilderInit.provisionInit(stack, userDataBuilder.build(cloudPlatform, stack.getHash(), new HashMap<String, String>()));
                    for (ResourceBuilder resourceBuilder : networkResourceBuilders.get(cloudPlatform)) {
                        pCO.getNetworkResources().addAll(stack.getResourcesByType(resourceBuilder.resourceType()));
                    }
                    List<Future<List<Resource>>> futures = new ArrayList<>();
                    Set<Resource> resourceSet = new HashSet<>();
                    for (int i = stack.getNodeCount(); i < stack.getNodeCount() + scalingAdjustment; i++) {
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
                    LOGGER.info("Publishing {} event.", ReactorConfig.ADD_INSTANCES_COMPLETE_EVENT);
                    reactor.notify(ReactorConfig.ADD_INSTANCES_COMPLETE_EVENT,
                            Event.wrap(new AddInstancesComplete(cloudPlatform, stack.getId(), resourceSet)));
                }
            } else {
                Set<String> instanceIds = new HashSet<>();
                int i = 0;
                for (InstanceMetaData metadataEntry : stack.getInstanceMetaData()) {
                    if (metadataEntry.isRemovable()) {
                        instanceIds.add(metadataEntry.getInstanceId());
                        if (++i >= scalingAdjustment * -1) {
                            break;
                        }
                    }
                }
                if (cloudPlatform.isWithTemplate()) {
                    cloudPlatformConnectors.get(cloudPlatform).removeInstances(stack, instanceIds);
                } else {
                    ResourceBuilderInit resourceBuilderInit = resourceBuilderInits.get(cloudPlatform);
                    final DeleteContextObject dCO = resourceBuilderInit.deleteInit(stack);

                    for (int j = instanceResourceBuilders.get(cloudPlatform).size() - 1; j >= 0; j--) {
                        List<Future<Boolean>> futures = new ArrayList<>();
                        final int index = j;
                        for (final String instanceId : instanceIds) {
                            Future<Boolean> submit = resourceBuilderExecutor.submit(new Callable<Boolean>() {
                                @Override
                                public Boolean call() throws Exception {
                                    Resource resource =
                                            new Resource(instanceResourceBuilders.get(cloudPlatform).get(index).resourceType(), instanceId, stack);
                                    Boolean delete = false;
                                    try {
                                        delete = instanceResourceBuilders.get(cloudPlatform).get(index).delete(resource, dCO);
                                    } catch (HttpResponseException ex) {
                                        LOGGER.error(String.format("Error occurred on stack under the instance remove"), ex);
                                        throw new InternalServerException(
                                                String.format("Error occurred while removing instance '%s' on stack. Message: '%s'",
                                                instanceId, ex.getResponse().toString()), ex);
                                    } catch (Exception ex) {
                                        throw new InternalServerException(
                                                String.format("Error occurred while removing instance '%s' on stack. Message: '%s'",
                                                instanceId, ex.getMessage()), ex);
                                    }
                                    return delete;
                                }
                            });
                            futures.add(submit);
                        }
                        for (Future<Boolean> future : futures) {
                            try {
                                future.get();
                            } catch (Exception ex) {
                                throw ex;
                            }
                        }
                    }
                    LOGGER.info("Terminated instances in stack: '{}'", instanceIds);
                    LOGGER.info("Publishing {} event.", ReactorConfig.STACK_UPDATE_SUCCESS_EVENT);
                    reactor.notify(ReactorConfig.STACK_UPDATE_SUCCESS_EVENT, Event.wrap(new StackUpdateSuccess(stack.getId(), true, instanceIds)));
                }
            }
        } catch (AddInstancesFailedException e) {
            LOGGER.error(e.getMessage(), e);
            notifyUpdateFailed(stack, e.getMessage());
        } catch (Exception e) {
            String errMessage = "Unhandled exception occurred while updating stack.";
            LOGGER.error(errMessage, e);
            notifyUpdateFailed(stack, errMessage);
        }
    }

    private void notifyUpdateFailed(Stack stack, String detailedMessage) {
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Publishing {} event.", ReactorConfig.STACK_UPDATE_FAILED_EVENT);
        reactor.notify(ReactorConfig.STACK_UPDATE_FAILED_EVENT, Event.wrap(new StackOperationFailure(stack.getId(), detailedMessage)));
    }
}
