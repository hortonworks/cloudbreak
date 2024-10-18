package com.sequenceiq.flow.core.metrics;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class FlowEventMetricListener<S, E> extends StateMachineListenerAdapter<S, E> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowEventMetricListener.class);

    @Inject
    private FlowMetricSender flowMetricSender;

    private final S finalState;

    private final String flowChainType;

    private final String flowType;

    private final long startTimeInMillis;

    public FlowEventMetricListener(S finalState, String flowChainType, String flowType, long startTimeInMillis) {
        this.finalState = finalState;
        this.flowChainType = flowChainType;
        this.flowType = flowType;
        this.startTimeInMillis = startTimeInMillis;
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
        flowMetricSender.send(flowType, flowChainType, nextFlowState, flowEvent, startTimeInMillis);
    }

    @Override
    public void stateMachineStopped(StateMachine<S, E> stateMachine) {
        LOGGER.info("State machine stopped: {}", stateMachine);
        flowMetricSender.send(flowType, flowChainType, finalState.toString(), null, startTimeInMillis);
    }
}
