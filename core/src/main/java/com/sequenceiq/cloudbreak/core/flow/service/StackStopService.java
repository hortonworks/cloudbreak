package com.sequenceiq.cloudbreak.core.flow.service;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.CloudPlatformResolver;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;

@Service
public class StackStopService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackStopService.class);

    @Inject
    private StackRepository stackRepository;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Inject
    private CloudPlatformResolver platformResolver;

    public boolean isStopPossible(FlowContext context) throws CloudbreakException {
        StackStatusUpdateContext stackStatusUpdateContext = (StackStatusUpdateContext) context;
        long stackId = stackStatusUpdateContext.getStackId();
        Stack stack = stackRepository.findOneWithLists(stackId);
        if (stack != null && stack.isStopRequested()) {
            return true;
        } else {
            LOGGER.info("Stack stop has not been requested, stop stack later.");
            return false;
        }
    }

    public FlowContext stop(FlowContext context) throws CloudbreakException {
        StackStatusUpdateContext stackStatusUpdateContext = (StackStatusUpdateContext) context;
        long stackId = stackStatusUpdateContext.getStackId();
        Stack stack = stackRepository.findOneWithLists(stackId);
        platformResolver.connector(stack.cloudPlatform()).stopAll(stack);
        return stackStatusUpdateContext;
    }

}
