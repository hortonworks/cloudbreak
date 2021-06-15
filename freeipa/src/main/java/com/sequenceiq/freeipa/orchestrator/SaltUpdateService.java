package com.sequenceiq.freeipa.orchestrator;

import static com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateEvent.SALT_UPDATE_EVENT;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class SaltUpdateService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SaltUpdateService.class);

    @Inject
    private FreeIpaFlowManager flowManager;

    @Inject
    private StackService stackService;

    public FlowIdentifier updateSaltStates(String environmentCrn, String accountId) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        MDCBuilder.buildMdcContext(stack);
        StackEvent event = new StackEvent(SALT_UPDATE_EVENT.event(), stack.getId());
        LOGGER.info("Triggering salt update flow with event: {}", event);
        return flowManager.notify(event.selector(), event);
    }
}
