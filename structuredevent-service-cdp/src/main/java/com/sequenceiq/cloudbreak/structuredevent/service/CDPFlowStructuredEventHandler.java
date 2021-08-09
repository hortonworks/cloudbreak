package com.sequenceiq.cloudbreak.structuredevent.service;

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

import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.flow.core.FlowEventListener;

@Primary
@Component
@Scope("prototype")
public class CDPFlowStructuredEventHandler<S, E> extends StateMachineListenerAdapter<S, E> implements FlowEventListener<S, E> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CDPFlowStructuredEventHandler.class);

    @Inject
    private CDPDefaultStructuredEventClient cdpDefaultStructuredEventClient;

    @Inject
    private CDPStructuredFlowEventFactory cdpStructuredFlowEventFactory;

    private final S initState;

    private final S finalState;

    private final String flowChainType;

    private final String flowChainId;

    private final String flowType;

    private final String flowId;

    private final Long resourceId;

    private Long lastStateChange;

    private Exception exception;

    public CDPFlowStructuredEventHandler(S initState, S finalState, String flowChainType, String flowType,
            String flowChainId, String flowId, Long resourceId) {
        this.initState = initState;
        this.finalState = finalState;
        this.flowType = flowType;
        this.flowId = flowId;
        this.flowChainType = flowChainType;
        this.flowChainId = flowChainId;
        this.resourceId = resourceId;
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

    /**
     * Send a new structured event.
     * @param transition provides information to build the new Structured Event.
     */
    @Override
    public void transition(Transition<S, E> transition) {
        try {
            String fromId = getFromId(transition);
            String toId = getToId(transition);
            String eventId = getEventId(transition);
            Boolean detailed = toId.equals(initState.toString()) || toId.equals(finalState.toString());

            Long currentTime = System.currentTimeMillis();
            long duration = lastStateChange == null ? 0L : currentTime - lastStateChange;

            CDPStructuredEvent structuredEvent = buildCdpStructuredEvent(fromId, toId, eventId, detailed, duration);
            cdpDefaultStructuredEventClient.sendStructuredEvent(structuredEvent);

            lastStateChange = currentTime;
        } catch (RuntimeException ex) {
            LOGGER.error("Error happened during structured flow event generation! The event won't be stored!", ex);
        }
    }

    private CDPStructuredEvent buildCdpStructuredEvent(String fromId, String toId, String eventId, Boolean detailed, long duration) {
        FlowDetails flowDetails = new FlowDetails(flowChainType, flowType, flowChainId, flowId, fromId, toId, eventId, duration);
        CDPStructuredEvent structuredEvent;
        if (exception == null) {
            structuredEvent = cdpStructuredFlowEventFactory.createStructuredFlowEvent(resourceId, flowDetails, detailed);
        } else {
            structuredEvent = cdpStructuredFlowEventFactory.createStructuredFlowEvent(resourceId, flowDetails, true, exception);
            exception = null;
        }
        return structuredEvent;
    }

    // provide protection against null values
    private String getEventId(Transition<S, E> transition) {
        Trigger<S, E> trigger = transition.getTrigger();
        return trigger != null ? trigger.getEvent().toString() : "unknown";
    }

    // provide protection against null values
    private String getToId(Transition<S, E> transition) {
        State<S, E> to = transition.getTarget();
        return to != null ? to.getId().toString() : "unknown";
    }

    // provide protection against null values
    private String getFromId(Transition<S, E> transition) {
        State<S, E> from = transition.getSource();
        return from != null ? from.getId().toString() : "unknown";
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
            FlowDetails flowDetails = new FlowDetails(flowChainType, flowType, flowChainId, flowId, fromId, "unknown", "FLOW_CANCEL",
                    lastStateChange == null ? 0L : currentTime - lastStateChange);
            CDPStructuredEvent structuredEvent = cdpStructuredFlowEventFactory.createStructuredFlowEvent(resourceId, flowDetails, true);
            cdpDefaultStructuredEventClient.sendStructuredEvent(structuredEvent);
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
