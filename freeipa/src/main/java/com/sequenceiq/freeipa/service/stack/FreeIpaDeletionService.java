package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationEvent.TERMINATION_EVENT;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.termination.event.TerminationEvent;
import com.sequenceiq.freeipa.service.FreeIpaFlowManager;

@Service
public class FreeIpaDeletionService {

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaFlowManager flowManager;

    public void delete(String accountId, String environment, String name) {
        Stack stack = stackService.getByAccountIdEnvironmentAndName(accountId, environment, name);
        flowManager.notify(TERMINATION_EVENT.event(), new TerminationEvent(TERMINATION_EVENT.event(), stack.getId(), false));
    }
}
