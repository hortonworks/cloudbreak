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
import com.sequenceiq.cloudbreak.domain.BillingStatus;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterInstallerMailSenderService;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.event.StackOperationFailure;
import com.sequenceiq.cloudbreak.service.stack.resource.DeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderInit;

import reactor.event.Event;
import reactor.function.Consumer;

@Service
public class StackCreationFailureHandler implements Consumer<Event<StackOperationFailure>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackCreationFailureHandler.class);

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Autowired
    private AmbariClusterInstallerMailSenderService ambariClusterInstallerMailSenderService;

    @Autowired
    private StackRepository stackRepository;

    @javax.annotation.Resource
    private Map<CloudPlatform, CloudPlatformConnector> cloudPlatformConnectors;

    @javax.annotation.Resource
    private Map<CloudPlatform, List<ResourceBuilder>> instanceResourceBuilders;

    @javax.annotation.Resource
    private Map<CloudPlatform, List<ResourceBuilder>> networkResourceBuilders;

    @javax.annotation.Resource
    private Map<CloudPlatform, ResourceBuilderInit> resourceBuilderInits;

    @Autowired
    private AsyncTaskExecutor resourceBuilderExecutor;

    @Autowired
    private CloudbreakEventService cloudbreakEventService;

    @Override
    public void accept(Event<StackOperationFailure> event) {
        StackOperationFailure stackCreationFailure = event.getData();
        Long stackId = stackCreationFailure.getStackId();
        String detailedMessage = stackCreationFailure.getDetailedMessage();
        stackUpdater.updateStackStatus(stackId, Status.CREATE_FAILED, detailedMessage);
        final Stack stack = stackRepository.findOneWithLists(stackId);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Accepted {} event.", ReactorConfig.STACK_CREATE_FAILED_EVENT, stackId);
        if (stack.getCluster().getEmailNeeded()) {
            ambariClusterInstallerMailSenderService.sendFailEmail(stack.getOwner());
        }
        final CloudPlatform cloudPlatform = stack.cloudPlatform();
        try {
            if (cloudPlatform.isWithTemplate()) {
                cloudPlatformConnectors.get(cloudPlatform).rollback(stackRepository.findOneWithLists(stackId), stack.getResources());
            } else {
                ResourceBuilderInit resourceBuilderInit = resourceBuilderInits.get(cloudPlatform);
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
                                return instanceResourceBuilders.get(cloudPlatform).get(index).rollback(resource, dCO, stack.getRegion());
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
            }
        } catch (Exception ex) {
            LOGGER.error(String.format("Stack rollback failed on {} stack: ", stack.getId()), ex);
        }
        stackUpdater.updateStackStatusReason(stackId, detailedMessage);
        cloudbreakEventService.fireCloudbreakEvent(stackId, BillingStatus.BILLING_STOPPED.name(), "Stack creation failed.");
    }

}
