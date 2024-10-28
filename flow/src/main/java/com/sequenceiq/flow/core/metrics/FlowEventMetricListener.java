package com.sequenceiq.flow.core.metrics;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.FlowEdgeConfig;

@Component
@Scope("prototype")
public class FlowEventMetricListener<S, E> extends StateMachineListenerAdapter<S, E> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowEventMetricListener.class);

    private final FlowEdgeConfig<S, E> edgeConfig;

    private final String flowChainType;

    private final String flowType;

    private final Class<? extends Enum> stateType;

    private final long startTimeInMillis;

    @Inject
    private FlowMetricSender flowMetricSender;

    public FlowEventMetricListener(FlowEdgeConfig<S, E> edgeConfig, String flowChainType, String flowType,
            Class<? extends Enum> stateType, long startTimeInMillis) {
        this.edgeConfig = edgeConfig;
        this.flowChainType = flowChainType;
        this.flowType = flowType;
        this.stateType = stateType;
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
        flowMetricSender.send(edgeConfig, flowType, flowChainType, stateType, startTimeInMillis, nextFlowState, flowEvent);
    }

}
