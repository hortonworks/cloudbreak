package com.sequenceiq.cloudbreak.core.flow2;

import java.util.List;

import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.access.StateMachineAccess;
import org.springframework.statemachine.support.DefaultStateMachineContext;

public class FlowAdapter<S extends FlowState, E> implements Flow {
    private final String flowId;
    private final StateMachine<S, E> flowMachine;
    private final StateConverter<S> stateConverter;
    private final EventConverter<E> eventConverter;
    private final MessageFactory<E> messageFactory;

    public FlowAdapter(String flowId, StateMachine<S, E> flowMachine, MessageFactory<E> messageFactory, StateConverter<S> stateConverter,
            EventConverter<E> eventConverter) {
        this.flowId = flowId;
        this.flowMachine = flowMachine;
        this.messageFactory = messageFactory;
        this.stateConverter = stateConverter;
        this.eventConverter = eventConverter;
    }

    public void initialize() {
        flowMachine.start();
    }

    public void initialize(String stateRepresentation) {
        final S state  = stateConverter.convert(stateRepresentation);
        flowMachine.stop();
        List<? extends StateMachineAccess<S, E>> withAllRegions = flowMachine.getStateMachineAccessor().withAllRegions();
        for (StateMachineAccess<S, E> access : withAllRegions) {
            access.resetStateMachine(new DefaultStateMachineContext<S, E>(state, null, null, null));
        }
        flowMachine.start();
    }

    public S getCurrentState() {
        return flowMachine.getState().getId();
    }

    public void sendEvent(String key, Object object) {
        flowMachine.sendEvent(messageFactory.createMessage(flowId, eventConverter.convert(key), object));
    }
}
