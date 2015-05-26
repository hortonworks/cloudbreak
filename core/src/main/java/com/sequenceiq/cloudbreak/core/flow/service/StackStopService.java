package com.sequenceiq.cloudbreak.core.flow.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;
import com.sequenceiq.cloudbreak.domain.BillingStatus;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;

@Service
public class StackStopService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackStopService.class);

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Autowired
    private CloudbreakEventService cloudbreakEventService;

    @javax.annotation.Resource
    private Map<CloudPlatform, CloudPlatformConnector> cloudPlatformConnectors;

    public FlowContext stop(FlowContext context) throws CloudbreakException {
        StackStatusUpdateContext stackStatusUpdateContext = (StackStatusUpdateContext) context;
        long stackId = stackStatusUpdateContext.getStackId();
        Stack stack = stackRepository.findOneWithLists(stackId);

        if (stack != null && Status.STOP_REQUESTED.equals(stack.getStatus())) {
            stackUpdater.updateStackStatus(stackId, Status.STOP_IN_PROGRESS, "Cluster infrastructure is stopping.");
            CloudPlatformConnector connector = cloudPlatformConnectors.get(stack.cloudPlatform());
            if (connector.stopAll(stack)) {
                LOGGER.info("Update stack state to: {}", Status.STOPPED);
                stackUpdater.updateStackStatus(stackId, Status.STOPPED, "Cluster infrastructure stopped successfully.");
                cloudbreakEventService.fireCloudbreakEvent(stackId, BillingStatus.BILLING_STOPPED.name(), "Cluster infrastructure stopped.");
            } else {
                throw new CloudbreakException("The cluster infrastructure could not be stopped.");
            }
        } else {
            LOGGER.info("Stack stop has not been requested, stop stack later.");
        }
        return stackStatusUpdateContext;
    }

    public FlowContext handleStackStopFailure(FlowContext context) {
        StackStatusUpdateContext stopContext = (StackStatusUpdateContext) context;
        LOGGER.info("Update stack state to: {}", Status.STOP_FAILED);
        stackUpdater.updateStackStatus(stopContext.getStackId(), Status.STOP_FAILED, stopContext.getErrorReason());
        return stopContext;
    }
}
