package com.sequenceiq.cloudbreak.core.flow.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.annotation.Resource;

import com.sequenceiq.cloudbreak.core.flow.ClusterStartService;
import com.sequenceiq.cloudbreak.core.flow.ClusterStopService;
import com.sequenceiq.cloudbreak.core.flow.StackStartService;
import com.sequenceiq.cloudbreak.core.flow.StackStopService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.FlowContextFactory;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;
import com.sequenceiq.cloudbreak.domain.BillingStatus;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.OnFailureAction;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterInstallerMailSenderService;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.flow.TerminationService;
import com.sequenceiq.cloudbreak.service.stack.resource.DeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderInit;

@Service
public class SimpleStackFacade implements StackFacade {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleStackFacade.class);

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Autowired
    private AmbariClusterInstallerMailSenderService ambariClusterInstallerMailSenderService;

    @Autowired
    private StackRepository stackRepository;

    @Resource
    private Map<CloudPlatform, CloudPlatformConnector> cloudPlatformConnectors;

    @Resource
    private Map<CloudPlatform, List<ResourceBuilder>> instanceResourceBuilders;

    @Resource
    private Map<CloudPlatform, List<ResourceBuilder>> networkResourceBuilders;

    @Resource
    private Map<CloudPlatform, ResourceBuilderInit> resourceBuilderInits;

    @Autowired
    private AsyncTaskExecutor resourceBuilderExecutor;

    @Autowired
    private CloudbreakEventService cloudbreakEventService;

    @Autowired
    private TerminationService terminationService;

    @Autowired
    private StackStartService stackStartService;

    @Autowired
    private StackStopService stackStopService;

    @Autowired
    private ClusterStartService clusterStartService;

    @Autowired
    private ClusterStopService clusterStopService;

    @Override
    public FlowContext stackCreationError(FlowContext context) throws CloudbreakException {
        ProvisioningContext provisioningContext = (ProvisioningContext) context;
        try {
            final Stack stack = stackRepository.findOneWithLists(provisioningContext.getStackId());
            final CloudPlatform cloudPlatform = provisioningContext.getCloudPlatform();

            if (!stack.getOnFailureActionAction().equals(OnFailureAction.ROLLBACK)) {
                LOGGER.debug("Nothing to do. OnFailureAction {}", stack.getOnFailureActionAction());
            } else {
                if (cloudPlatform.isWithTemplate()) {
                    cloudPlatformConnectors.get(cloudPlatform).rollback(stack, stack.getResources());
                } else {
                    ResourceBuilderInit resourceBuilderInit = resourceBuilderInits.get(cloudPlatform);
                    final DeleteContextObject dCO = resourceBuilderInit.deleteInit(stack);
                    for (int i = instanceResourceBuilders.get(cloudPlatform).size() - 1; i >= 0; i--) {
                        List<Future<Boolean>> futures = new ArrayList<>();
                        final int index = i;
                        List<com.sequenceiq.cloudbreak.domain.Resource> resourceByType =
                                stack.getResourcesByType(instanceResourceBuilders.get(cloudPlatform).get(i).resourceType());
                        for (final com.sequenceiq.cloudbreak.domain.Resource resource : resourceByType) {
                            Future<Boolean> submit = resourceBuilderExecutor.submit(new Callable<Boolean>() {
                                @Override
                                public Boolean call() throws Exception {
                                    instanceResourceBuilders.get(cloudPlatform).get(index).rollback(resource, dCO, stack.getRegion());
                                    stackUpdater.removeStackResources(stack.getId(), Arrays.asList(resource));
                                    return true;
                                }
                            });
                            futures.add(submit);
                        }
                        for (Future<Boolean> future : futures) {
                            future.get();
                        }
                    }
                    for (int i = networkResourceBuilders.get(cloudPlatform).size() - 1; i >= 0; i--) {
                        for (com.sequenceiq.cloudbreak.domain.Resource resource
                                : stack.getResourcesByType(networkResourceBuilders.get(cloudPlatform).get(i).resourceType())) {
                            networkResourceBuilders.get(cloudPlatform).get(i).rollback(resource, dCO, stack.getRegion());
                        }
                    }
                }
            }
            stackUpdater.updateStackStatusReason(provisioningContext.getStackId(), provisioningContext.getMessage());
            fireCloudbreakEventIfNeeded(provisioningContext.getStackId(), stack);
            return FlowContextFactory.createRollbackContext(provisioningContext.getStackId());
        } catch (Exception ex) {
            LOGGER.error(String.format("Stack rollback failed on {} stack: ", provisioningContext.getStackId()), ex);
            throw new CloudbreakException(String.format("Stack rollback failed on {} stack: ", provisioningContext.getStackId(), ex));
        }
    }

    @Override
    public FlowContext start(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Starting stack. Context: {}", context);
        try {
            context = stackStartService.start(context);
            LOGGER.debug("Starting stack is DONE.");
            return context;
        } catch (Exception e) {
            LOGGER.error("Exception during the stack start process: {}", e.getMessage());
            throw new CloudbreakException(e.getMessage(), e);
        }
    }

    @Override
    public FlowContext stop(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Stopping stack. Context: {}", context);
        try {
            context = stackStopService.stop(context);
            LOGGER.debug("Stopping stack is DONE.");
            return context;
        } catch (Exception e) {
            LOGGER.error("Exception during the stack stop process: {}", e.getMessage());
            throw new CloudbreakException(e.getMessage(), e);
        }
    }

    @Override
    public FlowContext terminateStack(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Terminating stack. Context: {}", context);
        try {
            context = terminationService.terminateStack(context);
            LOGGER.debug("Terminating stack is DONE");
            return context;
        } catch (Exception e) {
            LOGGER.error("Exception during the terminating process: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext stackTerminationError(FlowContext context) throws CloudbreakException {
        return terminationService.handleTerminationFailure(context);
    }

    @Override
    public FlowContext stackStopError(FlowContext context) throws CloudbreakException {
        return stackStopService.handleStackStopFailure(context);
    }

    @Override
    public FlowContext stackStartError(FlowContext context) throws CloudbreakException {
        return stackStartService.handleStackStartFailure(context);
    }

    private void fireCloudbreakEventIfNeeded(Long stackId, Stack stack) {
        if (stack.getOnFailureActionAction().equals(OnFailureAction.ROLLBACK)) {
            cloudbreakEventService.fireCloudbreakEvent(stackId, BillingStatus.BILLING_STOPPED.name(), "Stack creation failed.");
        }
    }

}
