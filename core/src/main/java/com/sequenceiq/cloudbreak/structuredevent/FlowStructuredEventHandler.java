package com.sequenceiq.cloudbreak.structuredevent;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.transition.Transition;
import org.springframework.statemachine.trigger.Trigger;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.FlowEventListener;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;

@Primary
@Component
@Scope("prototype")
public class FlowStructuredEventHandler<S, E> extends StateMachineListenerAdapter<S, E> implements FlowEventListener<S, E> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowStructuredEventHandler.class);

    @Inject
    private StructuredEventClient structuredEventClient;

    @Inject
    private StructuredFlowEventFactory structuredFlowEventFactory;

    private final S initState;

    private final S finalState;

    private final String flowType;

    private final String flowId;

    private final Long stackId;

    private Long lastStateChange;

    private Exception exception;

    public FlowStructuredEventHandler(S initState, S finalState, String flowType, String flowId, Long stackId) {
        this.initState = initState;
        this.finalState = finalState;
        this.flowType = flowType;
        this.flowId = flowId;
        this.stackId = stackId;
    }

    @Override
    public void stateChanged(State<S, E> from, State<S, E> to) {
    }

    @Override
    public void stateEntered(State<S, E> state) {

    }

    @Override
    public void stateExited(State<S, E> state) {

    }

    @Override
    public void eventNotAccepted(Message<E> event) {

    }

    @Override
    public void transition(Transition<S, E> transition) {
        try {
            State<S, E> from = transition.getSource();
            State<S, E> to = transition.getTarget();
            Trigger<S, E> trigger = transition.getTrigger();
            Long currentTime = System.currentTimeMillis();
            String fromId = from != null ? from.getId().toString() : "unknown";
            String toId = to != null ? to.getId().toString() : "unknown";
            String eventId = trigger != null ? trigger.getEvent().toString() : "unknown";
            Boolean detailed = toId.equals(initState.toString()) || toId.equals(finalState.toString());
            FlowDetails flowDetails = new FlowDetails("", flowType, "", flowId, fromId, toId, eventId,
                    lastStateChange == null ? 0L : currentTime - lastStateChange);
            StructuredEvent structuredEvent;
            if (exception == null) {
                structuredEvent = structuredFlowEventFactory.createStucturedFlowEvent(stackId, flowDetails, detailed);
            } else {
                structuredEvent = structuredFlowEventFactory.createStucturedFlowEvent(stackId, flowDetails, true, exception);
                exception = null;
            }
            structuredEventClient.sendStructuredEvent(structuredEvent);
            lastStateChange = currentTime;
        } catch (RuntimeException ex) {
            LOGGER.error("Error happened during structured flow event generation! The event won't be stored!", ex);
        }
    }

    @Override
    public void transitionStarted(Transition<S, E> transition) {

    }

    @Override
    public void transitionEnded(Transition<S, E> transition) {

    }

    @Override
    public void stateMachineStopped(StateMachine<S, E> stateMachine) {
        if (!stateMachine.isComplete()) {
            State<S, E> currentState = stateMachine.getState();
            Long currentTime = System.currentTimeMillis();
            String fromId = currentState != null ? currentState.getId().toString() : "unknown";
            FlowDetails flowDetails = new FlowDetails("", flowType, "", flowId, fromId, "unknown", "FLOW_CANCEL",
                    lastStateChange == null ? 0L : currentTime - lastStateChange);
            StructuredEvent structuredEvent = structuredFlowEventFactory.createStucturedFlowEvent(stackId, flowDetails, true);
            structuredEventClient.sendStructuredEvent(structuredEvent);
            lastStateChange = currentTime;
        }
    }

    @Override
    public void stateMachineError(StateMachine<S, E> stateMachine, Exception exception) {

    }

    public void setException(Exception exception) {
        this.exception = exception;
    }
}
