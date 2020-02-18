package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationEvent.TERMINATION_EVENT;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.controller.exception.NotFoundException;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.termination.event.TerminationEvent;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.sync.FreeipaJobService;

@Service
public class FreeIpaDeletionService {

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaFlowManager flowManager;

    @Inject
    private FreeipaJobService freeipaJobService;

    public void delete(String environmentCrn, String accountId) {
        List<Stack> stacks = stackService.findAllByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        if (stacks.isEmpty()) {
            throw new NotFoundException("No FreeIpa found in environment");
        }
        stacks.forEach(this::unscheduleAndTriggerTerminate);
    }

    private void unscheduleAndTriggerTerminate(Stack stack) {
        freeipaJobService.unschedule(stack);
        flowManager.notify(TERMINATION_EVENT.event(), new TerminationEvent(TERMINATION_EVENT.event(), stack.getId(), false));
        flowManager.cancelRunningFlows(stack.getId());
    }
}
