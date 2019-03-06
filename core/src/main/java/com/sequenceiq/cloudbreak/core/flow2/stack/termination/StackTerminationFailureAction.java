package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateContext;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.Flow;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component("StackTerminationFailureAction")
public class StackTerminationFailureAction extends AbstractStackFailureAction<StackTerminationState, StackTerminationEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackTerminationFailureAction.class);

    @Inject
    private StackTerminationService stackTerminationService;

    @Inject
    private StackService stackService;

    @Override
    protected StackFailureContext createFlowContext(
        String flowId, StateContext<StackTerminationState, StackTerminationEvent> stateContext, StackFailureEvent payload) {
        Flow flow = getFlow(flowId);
        StackView stackView = stackService.getViewByIdWithoutAuth(payload.getStackId());
        MDCBuilder.buildMdcContext(stackView);
        flow.setFlowFailed(payload.getException());
        return new StackFailureContext(flowId, stackView);
    }

    @Override
    protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
        Boolean deleteDependencies = Boolean.valueOf(String.valueOf(variables.get("DELETEDEPENDENCIES")));
        boolean forced = variables.get("FORCEDTERMINATION") != null && Boolean.valueOf(variables.get("FORCEDTERMINATION").toString());
        stackTerminationService.handleStackTerminationError(context.getStackView(), payload, forced, deleteDependencies);
        getMetricService().incrementMetricCounter(MetricType.STACK_TERMINATION_FAILED, context.getStackView());
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(StackFailureContext context) {
        return new StackEvent(StackTerminationEvent.STACK_TERMINATION_FAIL_HANDLED_EVENT.event(), context.getStackView().getId());
    }
}
