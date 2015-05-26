package com.sequenceiq.cloudbreak.core.flow.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;

@Service
public class StackStartService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackStartService.class);

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @javax.annotation.Resource
    private Map<CloudPlatform, CloudPlatformConnector> cloudPlatformConnectors;

    public FlowContext start(FlowContext context) throws CloudbreakException {
        StackStatusUpdateContext stackStatusUpdateContext = (StackStatusUpdateContext) context;
        Stack stack = stackRepository.findOneWithLists(stackStatusUpdateContext.getStackId());
        CloudPlatformConnector connector = cloudPlatformConnectors.get(stack.cloudPlatform());
        if (!connector.startAll(stack)) {
            throw new CloudbreakException("The cluster infrastructure cannot be started.");
        }
        return stackStatusUpdateContext;
    }

    public FlowContext handleStackStartFailure(FlowContext context) {
        StackStatusUpdateContext stackStatusUpdateContext = (StackStatusUpdateContext) context;
        LOGGER.info("Update stack state to: {}", Status.START_FAILED);
        stackUpdater.updateStackStatus(stackStatusUpdateContext.getStackId(), Status.START_FAILED, stackStatusUpdateContext.getErrorReason());
        return context;
    }
}
