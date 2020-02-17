package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationEvent.TERMINATION_EVENT;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.controller.exception.NotFoundException;
import com.sequenceiq.freeipa.entity.ChildEnvironment;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.termination.event.TerminationEvent;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.sync.FreeipaJobService;

@Service
public class FreeIpaDeletionService {

    @Inject
    private StackService stackService;

    @Inject
    private ChildEnvironmentService childEnvironmentService;

    @Inject
    private FreeIpaFlowManager flowManager;

    @Inject
    private FreeipaJobService freeipaJobService;

    public void delete(String environmentCrn, String accountId) {
        List<Stack> stacks = stackService.findAllByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        if (stacks.isEmpty()) {
            throw new NotFoundException("No FreeIpa found in environment");
        }
        stacks.forEach(stack -> validateDeletion(stack, accountId));
        stacks.forEach(this::unscheduleAndTriggerTerminate);
    }

    private void unscheduleAndTriggerTerminate(Stack stack) {
        freeipaJobService.unschedule(stack);
        flowManager.notify(TERMINATION_EVENT.event(), new TerminationEvent(TERMINATION_EVENT.event(), stack.getId(), false));
    }

    private void validateDeletion(Stack stack, String accountId) {
        List<ChildEnvironment> childEnvironments = childEnvironmentService.findChildEnvironments(stack, accountId);
        if (!childEnvironments.isEmpty()) {
            String childEnvironmentCrns = childEnvironments.stream()
                    .map(ChildEnvironment::getEnvironmentCrn)
                    .collect(Collectors.joining(", "));
            throw new BadRequestException(String.format("FreeIpa can not be deleted while it has the following child environment(s) attached [%s]",
                    childEnvironmentCrns));
        }
    }
}
