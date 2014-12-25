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

import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.controller.BuildStackFailureException;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
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
import com.sequenceiq.cloudbreak.service.stack.resource.CreateResourceRequest;
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
        final UpdateInstancesRequest request = event.getData();
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
                            .addInstances(stack, userDataBuilder.build(cloudPlatform, stack.getHash(), new HashMap<String, String>()),
                                    scalingAdjustment, request.getHostGroup());
                } else {
                    ResourceBuilderInit resourceBuilderInit = resourceBuilderInits.get(cloudPlatform);
                    final ProvisionContextObject pCO =
                            resourceBuilderInit.provisionInit(stack, userDataBuilder.build(cloudPlatform, stack.getHash(), new HashMap<String, String>()));
                    for (ResourceBuilder resourceBuilder : networkResourceBuilders.get(cloudPlatform)) {
                        pCO.getNetworkResources().addAll(stack.getResourcesByType(resourceBuilder.resourceType()));
                    }
                    List<Future<Boolean>> futures = new ArrayList<>();
                    final Set<Resource> resourceSet = new HashSet<>();
                    for (int i = stack.getFullNodeCount(); i < stack.getFullNodeCount() + scalingAdjustment; i++) {
                        final int index = i;
                        Future<Boolean> submit = resourceBuilderExecutor.submit(new Callable<Boolean>() {
                            @Override
                            public Boolean call() throws Exception {
                                List<Resource> resources = new ArrayList<>();
                                for (final ResourceBuilder resourceBuilder : instanceResourceBuilders.get(cloudPlatform)) {
                                    CreateResourceRequest createResourceRequest =
                                            resourceBuilder.buildCreateRequest(pCO,
                                                    resources,
                                                    resourceBuilder.buildResources(pCO, index, resources, stack.getTemplateAsGroup(request.getHostGroup())),
                                                    index,
                                                    stack.getTemplateAsGroup(request.getHostGroup()));
                                    stackUpdater.addStackResources(stack.getId(), createResourceRequest.getBuildableResources());
                                    resources.addAll(createResourceRequest.getBuildableResources());
                                    resourceSet.addAll(createResourceRequest.getBuildableResources());
                                    resourceBuilder.create(createResourceRequest, stack.getTemplateSetAsList().get(0), stack.getRegion());
                                }
                                return true;
                            }
                        });
                        futures.add(submit);
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

                    LOGGER.info("Publishing {} event.", ReactorConfig.ADD_INSTANCES_COMPLETE_EVENT);
                    reactor.notify(ReactorConfig.ADD_INSTANCES_COMPLETE_EVENT,
                            Event.wrap(new AddInstancesComplete(cloudPlatform, stack.getId(), resourceSet, request.getHostGroup())));
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
                    cloudPlatformConnectors.get(cloudPlatform).removeInstances(stack, instanceIds, request.getHostGroup());
                } else {
                    ResourceBuilderInit resourceBuilderInit = resourceBuilderInits.get(cloudPlatform);
                    final DeleteContextObject dCO = resourceBuilderInit.decommissionInit(stack, instanceIds);

                    for (int j = instanceResourceBuilders.get(cloudPlatform).size() - 1; j >= 0; j--) {
                        List<Future<Boolean>> futures = new ArrayList<>();
                        final int index = j;
                        final ResourceBuilder resourceBuilder = instanceResourceBuilders.get(cloudPlatform).get(index);
                        for (final Resource resource : getResourcesByType(resourceBuilder.resourceType(), dCO.getDecommisionResources())) {
                            Future<Boolean> submit = resourceBuilderExecutor.submit(new Callable<Boolean>() {
                                @Override
                                public Boolean call() throws Exception {
                                    Boolean delete = false;
                                    try {
                                        delete = resourceBuilder.delete(resource, dCO, stack.getRegion());
                                    } catch (HttpResponseException ex) {
                                        LOGGER.error(String.format("Error occurred on stack under the instance remove"), ex);
                                        throw new InternalServerException(
                                                String.format("Error occurred while removing instance '%s' on stack. Message: '%s'",
                                                        resource.getResourceName(), ex.getResponse().toString()), ex);
                                    } catch (Exception ex) {
                                        throw new InternalServerException(
                                                String.format("Error occurred while removing instance '%s' on stack. Message: '%s'",
                                                        resource.getResourceName(), ex.getMessage()), ex);
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
                    stackUpdater.removeStackResources(stackId, dCO.getDecommisionResources());
                    LOGGER.info("Terminated instances in stack: '{}'", instanceIds);
                    LOGGER.info("Publishing {} event.", ReactorConfig.STACK_UPDATE_SUCCESS_EVENT);
                    reactor.notify(ReactorConfig.STACK_UPDATE_SUCCESS_EVENT, Event.wrap(new StackUpdateSuccess(stack.getId(), true,
                            instanceIds, request.getHostGroup())));
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
