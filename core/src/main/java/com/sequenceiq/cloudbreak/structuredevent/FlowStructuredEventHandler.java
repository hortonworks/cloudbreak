package com.sequenceiq.cloudbreak.structuredevent;

import javax.inject.Inject;

import org.springframework.context.annotation.Scope;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.transition.Transition;
import org.springframework.statemachine.trigger.Trigger;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;

@Component
@Scope("prototype")
public class FlowStructuredEventHandler<S, E> extends StateMachineListenerAdapter<S, E> {
    @Inject
    private StructuredEventClient structuredEventClient;

    @Inject
    private StructuredFlowEventFactory structuredFlowEventFactory;

    private S initState;

    private S finalState;

    private String flowType;

    private String flowId;

    private Long stackId;

    private Long lastStateChange = -1L;

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
        State<S, E> from = transition.getSource();
        State<S, E> to = transition.getTarget();
        Trigger<S, E> trigger = transition.getTrigger();
        Long currentTime = System.currentTimeMillis();
        String fromId = from != null ? from.getId().toString() : "unknown";
        String toId = to != null ? to.getId().toString() : "unknown";
        String eventId = trigger != null ? trigger.getEvent().toString() : "unknown";
        Boolean detailed = toId.equals(initState.toString()) || toId.equals(finalState.toString());
        FlowDetails flowDetails = new FlowDetails("", flowType, "", flowId,  fromId, toId, eventId,
                lastStateChange == -1L ? -1L : currentTime - lastStateChange);
        StructuredEvent structuredEvent;
        if (exception == null) {
            structuredEvent = structuredFlowEventFactory.createStucturedFlowEvent(stackId, flowDetails, detailed);
        } else {
            structuredEvent = structuredFlowEventFactory.createStucturedFlowEvent(stackId, flowDetails, detailed, exception);
            exception = null;
        }
        structuredEventClient.sendStructuredEvent(structuredEvent);
        lastStateChange = currentTime;
    }

    @Override
    public void transitionStarted(Transition<S, E> transition) {

    }

    @Override
    public void transitionEnded(Transition<S, E> transition) {

    }

    @Override
    public void stateMachineError(StateMachine<S, E> stateMachine, Exception exception) {

    }

    public void setException(Exception exception) {
        this.exception = exception;
    }
}
