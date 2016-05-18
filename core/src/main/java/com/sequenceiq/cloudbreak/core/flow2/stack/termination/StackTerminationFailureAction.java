package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

@Component("StackTerminationFailureAction")
public class StackTerminationFailureAction extends AbstractStackFailureAction<StackTerminationState, StackTerminationEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackTerminationFailureAction.class);
    @Inject
    private StackTerminationService stackTerminationService;

    @Override
    protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
        stackTerminationService.handleStackTerminationError(context.getStack(), payload, variables.get("FORCEDTERMINATION") != null);
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(StackFailureContext context) {
        return new StackEvent(StackTerminationEvent.STACK_TERMINATION_FAIL_HANDLED_EVENT.stringRepresentation(), context.getStack().getId());
    }
}
