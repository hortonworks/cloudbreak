package com.sequenceiq.cloudbreak.service.stack.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.event.StackDeleteComplete;
import com.sequenceiq.cloudbreak.service.stack.event.StackDeleteRequest;
import com.sequenceiq.cloudbreak.service.stack.resource.DeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderInit;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderType;

import reactor.core.Reactor;
import reactor.event.Event;
import reactor.function.Consumer;

@Service
public class StackDeleteRequestHandler implements Consumer<Event<StackDeleteRequest>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackDeleteRequestHandler.class);

    @javax.annotation.Resource
    private Map<CloudPlatform, CloudPlatformConnector> cloudPlatformConnectors;

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private RetryingStackUpdater retryingStackUpdater;

    @javax.annotation.Resource
    private Map<CloudPlatform, Map<ResourceBuilderType, List<ResourceBuilder>>> resourceBuilders;

    @javax.annotation.Resource
    private Map<CloudPlatform, ResourceBuilderInit> resourceBuilderInits;

    @Autowired
    private Reactor reactor;

    @Override
    public void accept(Event<StackDeleteRequest> stackDeleteRequest) {
        StackDeleteRequest data = stackDeleteRequest.getData();
        LOGGER.info("Accepted {} event.", ReactorConfig.DELETE_REQUEST_EVENT, data.getStackId());
        retryingStackUpdater.updateStackStatus(data.getStackId(), Status.DELETE_IN_PROGRESS);
        Stack stack = stackRepository.findOneWithLists(data.getStackId());
        try {
            if (!data.getCloudPlatform().isWithTemplate()) {
                Map<ResourceBuilderType, List<ResourceBuilder>> resourceBuilderTypeListMap = resourceBuilders.get(data.getCloudPlatform());
                ResourceBuilderInit resourceBuilderInit = resourceBuilderInits.get(data.getCloudPlatform());
                final DeleteContextObject dCO = resourceBuilderInit.deleteInit(stack);

                final List<ResourceBuilder> resourceBuilders2 = resourceBuilderTypeListMap.get(ResourceBuilderType.INSTANCE_RESOURCE);
                ExecutorService executor = Executors.newFixedThreadPool(stack.getNodeCount());
                for (int i = resourceBuilders2.size() - 1; i >= 0; i--) {
                    List<Future<Boolean>> futures = new ArrayList<>();
                    final int index = i;
                    List<Resource> resourceByType = stack.getResourcesByType(resourceBuilders2.get(i).resourceType());
                    for (final Resource resource : resourceByType) {
                        Future<Boolean> submit = executor.submit(new Callable<Boolean>() {
                            @Override
                            public Boolean call() throws Exception {
                                return resourceBuilders2.get(index).delete(resource, dCO);
                            }
                        });
                        futures.add(submit);
                    }
                    for (Future<Boolean> future : futures) {
                        future.get();
                    }
                }
                List<ResourceBuilder> resourceBuilders1 = resourceBuilderTypeListMap.get(ResourceBuilderType.NETWORK_RESOURCE);
                for (int i = resourceBuilders2.size() - 1; i >= 0; i--) {
                    for (Resource resource : stack.getResourcesByType(resourceBuilders1.get(i).resourceType())) {
                        resourceBuilders1.get(i).delete(resource, dCO);
                    }
                }
                reactor.notify(ReactorConfig.DELETE_COMPLETE_EVENT, Event.wrap(new StackDeleteComplete(dCO.getStackId())));
            } else {
                cloudPlatformConnectors.get(data.getCloudPlatform()).deleteStack(stack, stack.getCredential());
            }
        } catch (Exception ex) {
            LOGGER.error(String.format("Stack delete failed on {} stack: ", stack.getId()), ex);
            retryingStackUpdater.updateStackStatus(data.getStackId(), Status.DELETE_FAILED);
        }
    }
}
