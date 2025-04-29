package com.sequenceiq.flow.core.listener;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.edh.FlowUsageSender;
import com.sequenceiq.flow.core.metrics.FlowMetricSender;

@Component
@Scope("prototype")
public class FlowEventCommonListener<S extends FlowState, E extends FlowEvent> extends StateMachineListenerAdapter<S, E> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowEventCommonListener.class);

    private final FlowTransitionContext<S, E> flowTransitionContext;

    @Inject
    private FlowMetricSender flowMetricSender;

    @Inject
    private FlowUsageSender flowUsageSender;

    public FlowEventCommonListener(FlowTransitionContext<S, E> flowTransitionContext) {
        this.flowTransitionContext = flowTransitionContext;
    }

    @Override
    public void transitionEnded(Transition<S, E> transition) {
        LOGGER.info("Transition ended: {}", transition);
        String nextFlowState = null;
        if (transition != null && transition.getTarget() != null && transition.getTarget().getId() != null) {
            nextFlowState = transition.getTarget().getId().toString();
        }
        String flowEvent = null;
        if (transition != null && transition.getTrigger() != null && transition.getTrigger().getEvent() != null) {
            flowEvent = transition.getTrigger().getEvent().toString();
        }
        flowMetricSender.send(flowTransitionContext, nextFlowState, flowEvent);
        flowUsageSender.send(flowTransitionContext, nextFlowState, flowEvent);
    }

}
