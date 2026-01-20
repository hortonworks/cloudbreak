package com.sequenceiq.freeipa.flow.stack.termination.action;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateContext;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.AbstractStackFailureAction;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.StackFailureContext;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.flow.stack.termination.StackTerminationEvent;
import com.sequenceiq.freeipa.flow.stack.termination.StackTerminationState;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component("StackTerminationFailureAction")
public class StackTerminationFailureAction extends AbstractStackFailureAction<StackTerminationState, StackTerminationEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackTerminationFailureAction.class);

    @Inject
    private StackTerminationService stackTerminationService;

    @Inject
    private StackService stackService;

    @Override
    protected StackFailureContext createFlowContext(FlowParameters flowParameters, StateContext<StackTerminationState, StackTerminationEvent> stateContext,
            StackFailureEvent payload) {
        Stack stack = stackService.getStackById(payload.getResourceId());
        MDCBuilder.buildMdcContext(stack);
        return new StackFailureContext(flowParameters, stack);
    }

    @Override
    protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
        boolean forced = variables.get("FORCEDTERMINATION") != null && Boolean.parseBoolean(variables.get("FORCEDTERMINATION").toString());
        stackTerminationService.handleStackTerminationError(context.getStack(), payload, forced);
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(StackFailureContext context) {
        return new StackEvent(StackTerminationEvent.STACK_TERMINATION_FAIL_HANDLED_EVENT.event(), context.getStack().getId());
    }
}
