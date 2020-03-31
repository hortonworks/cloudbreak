package com.sequenceiq.freeipa.flow.stack.termination.action;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.flow.stack.termination.StackTerminationContext;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@Service
public class StackTerminationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackTerminationService.class);

    @Inject
    private TerminationService terminationService;

    @Inject
    private StackUpdater stackUpdater;

    public void finishStackTermination(StackTerminationContext context, TerminateStackResult payload) {
        LOGGER.debug("Terminate stack result: {}", payload);
        Stack stack = context.getStack();
        terminationService.finalizeTermination(stack.getId());
    }

    public void handleStackTerminationError(Stack stack, StackFailureEvent payload, boolean forced) {
        if (!forced) {
            Exception errorDetails = payload.getException();
            String stackUpdateMessage = "Termination failed: " + errorDetails.getMessage();
            stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.DELETE_FAILED, stackUpdateMessage);
            LOGGER.debug("Error during stack termination flow: ", errorDetails);
        } else {
            terminationService.finalizeTermination(stack.getId());
        }
    }
}
