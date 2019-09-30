package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.freeipa.flow.stack.stop.StackStopEvent.STACK_STOP_EVENT;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.controller.exception.NotFoundException;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

@Service
public class FreeIpaStopService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaStopService.class);

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaFlowManager flowManager;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    public void stop(String environmentCrn, String accountId) {
        List<Stack> stacks = stackService.findAllByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        if (stacks.isEmpty()) {
            throw new NotFoundException("No FreeIpa found in environment");
        }

        stacks.stream()
                .filter(s -> s.isAvailable() || s.isStopFailed())
                .forEach(this::triggerStackStopIfNeeded);
    }

    private void triggerStackStopIfNeeded(Stack stack) {
        if (!isStopNeeded(stack)) {
            return;
        }
        stackUpdater.updateStackStatus(stack, DetailedStackStatus.STOP_REQUESTED, "Stopping of stack infrastructure has been requested.");
        flowManager.notify(STACK_STOP_EVENT.event(), new StackEvent(STACK_STOP_EVENT.event(), stack.getId()));
    }

    private boolean isStopNeeded(Stack stack) {
        boolean result = true;
        if (stack.isStopped()) {
            LOGGER.debug("Stack stop is ignored");
            result = false;
        } else if (!stack.isAvailable() && !stack.isStopFailed()) {
            throw new BadRequestException(
                    String.format("Cannot update the status of stack '%s' to STOPPED, because it isn't in AVAILABLE state.", stack.getName()));
        }
        return result;
    }
}
