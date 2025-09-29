package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.START_REQUESTED;
import static com.sequenceiq.freeipa.flow.stack.start.StackStartEvent.STACK_START_EVENT;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;

@Service
public class FreeIpaStartService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaStartService.class);

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaFlowManager flowManager;

    public void start(String environmentCrn, String accountId) {
        MDCBuilder.addEnvCrn(environmentCrn);
        MDCBuilder.addAccountId(accountId);
        List<Stack> stacks = stackService.findAllByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        if (stacks.isEmpty()) {
            LOGGER.debug("No FreeIPA found in environment");
            throw new NotFoundException("No FreeIPA found in environment");
        }
        stacks.stream()
                .filter(s -> s.getStackStatus().getStatus().isStartable())
                .forEach(this::triggerStackStart);
    }

    private void triggerStackStart(Stack stack) {
        MDCBuilder.buildMdcContext(stack);
        LOGGER.debug("Trigger start event, new status: {}", START_REQUESTED);
        flowManager.notify(STACK_START_EVENT.event(), new StackEvent(STACK_START_EVENT.event(), stack.getId()));
    }

}
