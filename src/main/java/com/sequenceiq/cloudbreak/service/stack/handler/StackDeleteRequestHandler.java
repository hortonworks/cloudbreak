package com.sequenceiq.cloudbreak.service.stack.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.event.StackDeleteComplete;
import com.sequenceiq.cloudbreak.service.stack.event.StackDeleteRequest;
import com.sequenceiq.cloudbreak.service.stack.flow.ProvisionUtil;
import com.sequenceiq.cloudbreak.service.stack.resource.DeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderInit;

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
    private Map<CloudPlatform, List<ResourceBuilder>> instanceResourceBuilders;

    @javax.annotation.Resource
    private Map<CloudPlatform, List<ResourceBuilder>> networkResourceBuilders;

    @javax.annotation.Resource
    private Map<CloudPlatform, ResourceBuilderInit> resourceBuilderInits;

    @Autowired
    private Reactor reactor;

    @Autowired
    private AsyncTaskExecutor resourceBuilderExecutor;

    @Autowired
    private ProvisionUtil provisionUtil;

    @Override
    public void accept(Event<StackDeleteRequest> stackDeleteRequest) {
        final StackDeleteRequest data = stackDeleteRequest.getData();
        retryingStackUpdater.updateStackStatus(data.getStackId(), Status.DELETE_IN_PROGRESS, "Termination of cluster infrastructure has started.");
        final Stack stack = stackRepository.findOneWithLists(data.getStackId());
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Accepted {} event.", ReactorConfig.DELETE_REQUEST_EVENT);
        try {
            if (!data.getCloudPlatform().isWithTemplate()) {
                ResourceBuilderInit resourceBuilderInit = resourceBuilderInits.get(data.getCloudPlatform());
                final DeleteContextObject dCO = resourceBuilderInit.deleteInit(stack);

                for (int i = instanceResourceBuilders.get(data.getCloudPlatform()).size() - 1; i >= 0; i--) {
                    List<Future<Boolean>> futures = new ArrayList<>();
                    final int index = i;
                    List<Resource> resourceByType = stack.getResourcesByType(instanceResourceBuilders.get(data.getCloudPlatform()).get(i).resourceType());
                    for (final Resource resource : resourceByType) {
                        Future<Boolean> submit = resourceBuilderExecutor.submit(new Callable<Boolean>() {
                            @Override
                            public Boolean call() throws Exception {
                                return instanceResourceBuilders.get(data.getCloudPlatform()).get(index).delete(resource, dCO, stack.getRegion());
                            }
                        });
                        futures.add(submit);
                        if (provisionUtil.isRequestFull(stack, futures.size() + 1)) {
                            provisionUtil.waitForRequestToFinish(stack.getId(), futures);
                        }
                    }
                    for (Future<Boolean> future : futures) {
                        future.get();
                    }
                }
                for (int i = networkResourceBuilders.get(data.getCloudPlatform()).size() - 1; i >= 0; i--) {
                    for (Resource resource : stack.getResourcesByType(networkResourceBuilders.get(data.getCloudPlatform()).get(i).resourceType())) {
                        networkResourceBuilders.get(data.getCloudPlatform()).get(i).delete(resource, dCO, stack.getRegion());
                    }
                }
                reactor.notify(ReactorConfig.DELETE_COMPLETE_EVENT, Event.wrap(new StackDeleteComplete(dCO.getStackId())));
            } else {
                cloudPlatformConnectors.get(data.getCloudPlatform()).deleteStack(stack, stack.getCredential());
            }
        } catch (Exception ex) {
            LOGGER.error(String.format("Stack delete failed on {} stack: ", stack.getId()), ex.getMessage());
            retryingStackUpdater.updateStackStatus(data.getStackId(), Status.DELETE_FAILED, "Termination of cluster infrastructure failed: " + ex.getMessage());
        }
    }

}
