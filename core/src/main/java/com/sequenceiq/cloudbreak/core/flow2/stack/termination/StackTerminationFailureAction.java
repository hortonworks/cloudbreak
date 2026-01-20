package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.AbstractStackTerminationAction.TERMINATION_TYPE;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateContext;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.TerminationType;
import com.sequenceiq.cloudbreak.service.recovery.RecoveryTeardownService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.FlowParameters;

@Component("StackTerminationFailureAction")
public class StackTerminationFailureAction extends AbstractStackFailureAction<StackTerminationState, StackTerminationEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackTerminationFailureAction.class);

    @Inject
    private StackTerminationService stackTerminationService;

    @Inject
    private RecoveryTeardownService recoveryTeardownService;

    @Inject
    private StackDtoService stackDtoService;

    @Override
    protected StackFailureContext createFlowContext(FlowParameters flowParameters, StateContext<StackTerminationState, StackTerminationEvent> stateContext,
            StackFailureEvent payload) {
        StackView stack = stackDtoService.getStackViewById(payload.getResourceId());
        MDCBuilder.buildMdcContext(stack);
        return new StackFailureContext(flowParameters, stack, payload.getResourceId());
    }

    @Override
    protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
        TerminationType terminationType = (TerminationType) variables.getOrDefault(TERMINATION_TYPE, TerminationType.REGULAR);
        boolean forced = terminationType.isForced();
        boolean recovery = terminationType.isRecovery();
        Exception payloadException = payload.getException();
        StackView stack = stackDtoService.getStackViewById(context.getStackId());

        try {
            if (recovery) {
                recoveryTeardownService.handleRecoveryTeardownError(stack, payloadException);
            } else {
                stackTerminationService.handleStackTerminationError(stack, payloadException, forced);
            }
        } catch (Exception e) {
            LOGGER.error("Exception occurred while Cloudbreak tried to handle stack {} termination.", terminationType.name(), e);
        }
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(StackFailureContext context) {
        return new StackEvent(StackTerminationEvent.STACK_TERMINATION_FAIL_HANDLED_EVENT.event(), context.getStackId());
    }
}
