package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.freeipa.flow.stack.start.StackStartEvent.STACK_START_EVENT;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;

@Service
public class FreeIpaStartService {

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaFlowManager flowManager;

    public void start(String environmentCrn, String accountId) {
        List<Long> stacks = stackService.findAllIdByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        if (stacks.isEmpty()) {
            throw new NotFoundException("No FreeIpa found in environment");
        }

        stacks.forEach(stackId -> flowManager.notify(STACK_START_EVENT.event(), new StackEvent(STACK_START_EVENT.event(), stackId)));
    }
}
