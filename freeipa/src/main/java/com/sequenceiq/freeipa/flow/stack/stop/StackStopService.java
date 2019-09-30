package com.sequenceiq.freeipa.flow.stack.stop;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesResult;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.flow.stack.StackStartStopService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@Component
public class StackStopService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackStopService.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private StackStartStopService stackStartStopService;

    public void startStackStop(StackStopContext context) {
        stackUpdater.updateStackStatus(context.getStack(), DetailedStackStatus.STOP_IN_PROGRESS, "Stack infrastructure is now stopping.");
    }

    public void handleStackStopError(Stack stack, StackFailureEvent payload) {
        String logMessage = "Stack stop failed: ";
        LOGGER.info(logMessage, payload.getException());
        stackUpdater.updateStackStatus(stack, DetailedStackStatus.STOP_FAILED, logMessage + payload.getException().getMessage());
    }

    public void finishStackStop(StackStopContext context, StopInstancesResult stopInstancesResult) {
        Stack stack = context.getStack();
        stackStartStopService.validateResourceResults(context.getCloudContext(),
                stopInstancesResult.getErrorDetails(), stopInstancesResult.getResults(), false);
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.STOPPED, "Stack infrastructure stopped successfully.");
    }

    public boolean isStopPossible(Stack stack) {
        if (stack != null && stack.isStopRequested()) {
            return true;
        } else {
            LOGGER.debug("Stack stop has not been requested because stack isn't in stop requested state, stop stack later.");
            return false;
        }
    }
}
