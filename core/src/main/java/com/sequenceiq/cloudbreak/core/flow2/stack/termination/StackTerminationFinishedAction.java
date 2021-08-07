package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.recovery.RecoveryTeardownService;

@Component("StackTerminationFinishedAction")
public class StackTerminationFinishedAction extends AbstractStackTerminationAction<TerminateStackResult> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackTerminationFinishedAction.class);

    @Inject
    private StackTerminationService stackTerminationService;

    @Inject
    private RecoveryTeardownService recoveryTeardownService;

    public StackTerminationFinishedAction() {
        super(TerminateStackResult.class);
    }

    @Override
    protected void doExecute(StackTerminationContext context, TerminateStackResult payload, Map<Object, Object> variables) {
        if (context.getTerminationType().isRecovery()) {
            LOGGER.debug("Recovery is in progress, skipping stack termination finalization!");
            recoveryTeardownService.handleRecoveryTeardownSuccess(context, payload);
        } else {
            LOGGER.debug("Termination type is {}, executing stack termination finalization!", context.getTerminationType());
            stackTerminationService.finishStackTermination(context, payload);
        }
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(StackTerminationContext context) {
        return new StackEvent(StackTerminationEvent.TERMINATION_FINALIZED_EVENT.event(), context.getStack().getId());
    }
}
