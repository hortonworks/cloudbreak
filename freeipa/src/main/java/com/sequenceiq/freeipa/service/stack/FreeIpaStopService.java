package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.freeipa.flow.stack.stop.StackStopEvent.STACK_STOP_EVENT;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.api.model.FlowIdentifier;
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

    public Optional<FlowIdentifier> stop(String environmentCrn, String accountId) {
        MDCBuilder.addEnvCrn(environmentCrn);
        MDCBuilder.addAccountId(accountId);
        List<Stack> stacks = stackService.findAllByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        if (stacks.isEmpty()) {
            LOGGER.info("No FreeIPA found in environment");
            throw new NotFoundException("No FreeIPA found in environment");
        }

        Optional<Stack> firstExistingFreeIpa = stacks.stream()
                .filter(s -> s.isAvailable() || s.isStopFailed())
                .findFirst();
        if (firstExistingFreeIpa.isPresent()) {
            return triggerStackStopIfNeeded(firstExistingFreeIpa.get());
        }
        return Optional.empty();
    }

    private Optional<FlowIdentifier> triggerStackStopIfNeeded(Stack stack) {
        MDCBuilder.buildMdcContext(stack);
        if (isStopNeeded(stack)) {
            LOGGER.debug("Trigger stop event, stack status: {}", stack.getStackStatus());
            return Optional.ofNullable(
                    flowManager.notify(
                        STACK_STOP_EVENT.event(),
                        new StackEvent(STACK_STOP_EVENT.event(), stack.getId())
                    )
            );
        }
        return Optional.empty();
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
