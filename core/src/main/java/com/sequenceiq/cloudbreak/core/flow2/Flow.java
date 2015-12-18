package com.sequenceiq.cloudbreak.core.flow2;

import org.springframework.statemachine.StateMachine;

public class Flow<S, E> {
    private String flowId;
    private StateMachine<S, E> flowMachine;
    private EventConverter<E> eventConverter;
    private MessageFactory<E> messageFactory;

    public Flow(StateMachine<S, E> flowMachine, MessageFactory<E> messageFactory, EventConverter<E> eventConverter) {
        this.flowMachine = flowMachine;
        this.messageFactory = messageFactory;
        this.eventConverter = eventConverter;
    }

    public void initialize(String flowId) {
        this.flowId = flowId;
    }

    public void start() {
        flowMachine.start();
    }

    public void sendEvent(String key, Object object) {
        flowMachine.sendEvent(messageFactory.createMessage(flowId, eventConverter.convert(key), object));
    }
}
