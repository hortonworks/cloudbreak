package com.sequenceiq.cloudbreak.core.flow2;

import java.util.List;

import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.access.StateMachineAccess;
import org.springframework.statemachine.support.DefaultStateMachineContext;

import com.sequenceiq.cloudbreak.core.flow2.config.FlowConfiguration;

public class FlowAdapter<S extends FlowState, E> implements Flow {
    private final String flowId;

    private final StateMachine<S, E> flowMachine;

    private final StateConverter<S> stateConverter;

    private final EventConverter<E> eventConverter;

    private final MessageFactory<E> messageFactory;

    private boolean flowFailed;

    private final Class<? extends FlowConfiguration> flowConfigClass;

    public FlowAdapter(String flowId, StateMachine<S, E> flowMachine, MessageFactory<E> messageFactory, StateConverter<S> stateConverter,
            EventConverter<E> eventConverter, Class<? extends FlowConfiguration> flowConfigClass) {
        this.flowId = flowId;
        this.flowMachine = flowMachine;
        this.messageFactory = messageFactory;
        this.stateConverter = stateConverter;
        this.eventConverter = eventConverter;
        this.flowConfigClass = flowConfigClass;
    }

    public void initialize() {
        flowMachine.start();
    }

    public void initialize(String stateRepresentation) {
        final S state  = stateConverter.convert(stateRepresentation);
        flowMachine.stop();
        List<? extends StateMachineAccess<S, E>> withAllRegions = flowMachine.getStateMachineAccessor().withAllRegions();
        for (StateMachineAccess<S, E> access : withAllRegions) {
            access.resetStateMachine(new DefaultStateMachineContext<>(state, null, null, null));
        }
        flowMachine.start();
    }

    public S getCurrentState() {
        return flowMachine.getState().getId();
    }

    @Override
    public Class<? extends FlowConfiguration> getFlowConfigClass() {
        return flowConfigClass;
    }

    public void sendEvent(String key, Object payload) {
        flowMachine.sendEvent(messageFactory.createMessage(flowId, eventConverter.convert(key), payload));
    }

    @Override
    public String getFlowId() {
        return flowId;
    }

    @Override
    public void setFlowFailed() {
        this.flowFailed = true;
    }

    @Override
    public boolean isFlowFailed() {
        return flowFailed;
    }
}
