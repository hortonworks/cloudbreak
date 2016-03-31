package com.sequenceiq.cloudbreak.core.flow2;

import org.springframework.statemachine.StateMachine;

public class Flow<S, E> {
    private final String flowId;
    private final StateMachine<S, E> flowMachine;
    private final EventConverter<E> eventConverter;
    private final MessageFactory<E> messageFactory;

    public Flow(String flowId, StateMachine<S, E> flowMachine, MessageFactory<E> messageFactory, EventConverter<E> eventConverter) {
        this.flowId = flowId;
        this.flowMachine = flowMachine;
        this.messageFactory = messageFactory;
        this.eventConverter = eventConverter;
    }

    public void start() {
        flowMachine.start();
    }

    public void sendEvent(String key, Object object) {
        flowMachine.sendEvent(messageFactory.createMessage(flowId, eventConverter.convert(key), object));
    }
}
