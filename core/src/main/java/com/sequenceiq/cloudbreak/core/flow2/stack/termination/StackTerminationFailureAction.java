package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateContext;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.common.type.MetricType;
import com.sequenceiq.cloudbreak.core.flow2.Flow;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
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
        Stack stack = stackService.getByIdWithLists(payload.getStackId());
        MDCBuilder.buildMdcContext(stack);
        flow.setFlowFailed(payload.getException());
        return new StackFailureContext(flowId, stack);
    }

    @Override
    protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
        Boolean deleteDependencies = Boolean.valueOf(String.valueOf(variables.get("DELETEDEPENDENCIES")));
        stackTerminationService.handleStackTerminationError(context.getStack(), payload,
            variables.get("FORCEDTERMINATION") != null, deleteDependencies);
        metricService.incrementMetricCounter(MetricType.STACK_TERMINATION_FAILED, context.getStackView());
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(StackFailureContext context) {
        return new StackEvent(StackTerminationEvent.STACK_TERMINATION_FAIL_HANDLED_EVENT.event(), context.getStackView().getId());
    }
}
