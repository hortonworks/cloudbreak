package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.AbstractStackTerminationAction.TERMINATION_TYPE;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateContext;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.TerminationType;
import com.sequenceiq.cloudbreak.service.recovery.RecoveryTeardownService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;

@Component("StackTerminationFailureAction")
public class StackTerminationFailureAction extends AbstractStackFailureAction<StackTerminationState, StackTerminationEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackTerminationFailureAction.class);

    @Inject
    private StackTerminationService stackTerminationService;

    @Inject
    private RecoveryTeardownService recoveryTeardownService;

    @Inject
    private StackService stackService;

    @Override
    protected StackFailureContext createFlowContext(FlowParameters flowParameters, StateContext<StackTerminationState, StackTerminationEvent> stateContext,
            StackFailureEvent payload) {
        Flow flow = getFlow(flowParameters.getFlowId());
        StackView stackView = stackService.getViewByIdWithoutAuth(payload.getResourceId());
        MDCBuilder.buildMdcContext(stackView);
        flow.setFlowFailed(payload.getException());
        return new StackFailureContext(flowParameters, stackView);
    }

    @Override
    protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
        TerminationType terminationType = (TerminationType) variables.getOrDefault(TERMINATION_TYPE, TerminationType.REGULAR);
        boolean forced = terminationType.isForced();
        boolean recovery = terminationType.isRecovery();
        Exception payloadException = payload.getException();
        StackView stackView = context.getStackView();

        try {
            if (recovery) {
                recoveryTeardownService.handleRecoveryTeardownError(stackView, payloadException);
            } else {
                stackTerminationService.handleStackTerminationError(stackView, payloadException, forced);
            }
        } catch (Exception e) {
            LOGGER.error("Exception occurred while Cloudbreak tried to handle stack {} termination.", terminationType.name(), e);
        }
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(StackFailureContext context) {
        return new StackEvent(StackTerminationEvent.STACK_TERMINATION_FAIL_HANDLED_EVENT.event(), context.getStackView().getId());
    }
}
