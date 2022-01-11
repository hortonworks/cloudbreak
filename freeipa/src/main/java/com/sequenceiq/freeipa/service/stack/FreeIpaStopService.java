package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.freeipa.flow.stack.stop.StackStopEvent.STACK_STOP_EVENT;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;

@Service
public class FreeIpaStopService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaStopService.class);

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaFlowManager flowManager;

    @Inject
    private StackUpdater stackUpdater;

    public void stop(String environmentCrn, String accountId) {
        MDCBuilder.addEnvCrn(environmentCrn);
        MDCBuilder.addAccountId(accountId);
        List<Stack> stacks = stackService.findAllByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        if (stacks.isEmpty()) {
            LOGGER.info("No FreeIPA found in environment");
            throw new NotFoundException("No FreeIPA found in environment");
        }

        stacks.stream()
                .filter(s -> s.isAvailable() || s.isStopFailed())
                .forEach(this::triggerStackStopIfNeeded);
    }

    private void triggerStackStopIfNeeded(Stack stack) {
        MDCBuilder.buildMdcContext(stack);
        if (!isStopNeeded(stack)) {
            return;
        }
        LOGGER.debug("Trigger stop event, new status: {}", DetailedStackStatus.STOP_REQUESTED);
        stackUpdater.updateStackStatus(stack, DetailedStackStatus.STOP_REQUESTED, "Stopping of stack infrastructure has been requested.");
        flowManager.notify(STACK_STOP_EVENT.event(), new StackEvent(STACK_STOP_EVENT.event(), stack.getId()));
    }

    private boolean isStopNeeded(Stack stack) {
        boolean result = true;
        if (stack.isStopped()) {
            LOGGER.debug("Stack stop is ignored");
            result = false;
        } else if (!stack.isAvailable() && !stack.isStopFailed()) {
            LOGGER.debug("Cannot update the status of stack '{}' to STOPPED, because it isn't in AVAILABLE state.", stack.getName());
            throw new BadRequestException(
                    String.format("Cannot update the status of stack '%s' to STOPPED, because it isn't in AVAILABLE state.", stack.getName()));
        }
        return result;
    }

}
