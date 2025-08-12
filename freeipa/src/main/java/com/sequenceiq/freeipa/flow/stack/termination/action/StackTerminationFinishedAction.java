package com.sequenceiq.freeipa.flow.stack.termination.action;

import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.termination.StackTerminationContext;
import com.sequenceiq.freeipa.flow.stack.termination.StackTerminationEvent;
import com.sequenceiq.freeipa.service.config.AbstractConfigRegister;
import com.sequenceiq.freeipa.service.crossrealm.CrossRealmTrustService;
import com.sequenceiq.freeipa.service.freeipa.user.AuthDistributorService;

@Component("StackTerminationFinishedAction")
public class StackTerminationFinishedAction extends AbstractStackTerminationAction<TerminateStackResult> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackTerminationFinishedAction.class);

    @Inject
    private StackTerminationService stackTerminationService;

    @Inject
    private Set<AbstractConfigRegister> configRegisters;

    @Inject
    private AuthDistributorService authDistributorService;

    @Inject
    private CrossRealmTrustService crossRealmTrustService;

    public StackTerminationFinishedAction() {
        super(TerminateStackResult.class);
    }

    @Override
    protected void doExecute(StackTerminationContext context, TerminateStackResult payload, Map<Object, Object> variables) {
        Stack stack = context.getStack();
        LOGGER.info("Stack termination finished for stack: {}", stack.getId());
        configRegisters.forEach(configProvider -> configProvider.delete(stack));
        authDistributorService.removeAuthViewForEnvironment(stack.getEnvironmentCrn(), stack.getAccountId());
        crossRealmTrustService.deleteByStackIdIfExists(stack.getId());
        stackTerminationService.finishStackTermination(context, payload);
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(StackTerminationContext context) {
        return new StackEvent(StackTerminationEvent.TERMINATION_FINALIZED_EVENT.event(), context.getStack().getId());
    }
}
