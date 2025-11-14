package com.sequenceiq.flow.core;

import java.util.List;
import java.util.Map;

import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.access.StateMachineAccess;
import org.springframework.statemachine.support.DefaultStateMachineContext;

import com.sequenceiq.flow.core.config.FlowConfiguration;

public class FlowAdapter<S extends FlowState, E extends FlowEvent> implements Flow {
    private final String flowId;

    private final StateMachine<S, E> flowMachine;

    private final StateConverter<S> stateConverter;

    private final EventConverter<E> eventConverter;

    private final MessageFactory<E> messageFactory;

    private final Class<? extends FlowConfiguration<E>> flowConfigClass;

    private final FlowEventListener<S, E> flowEventListener;

    private boolean flowFailed;

    public FlowAdapter(String flowId, StateMachine<S, E> flowMachine, MessageFactory<E> messageFactory, StateConverter<S> stateConverter,
            EventConverter<E> eventConverter, Class<? extends FlowConfiguration<E>> flowConfigClass, FlowEventListener<S, E> flowEventListener) {
        this.flowId = flowId;
        this.flowMachine = flowMachine;
        this.messageFactory = messageFactory;
        this.stateConverter = stateConverter;
        this.eventConverter = eventConverter;
        this.flowConfigClass = flowConfigClass;
        this.flowEventListener = flowEventListener;
    }

    @Override
    public void initialize(Map<Object, Object> variables) {
        if (variables != null) {
            flowMachine.getExtendedState().getVariables().putAll(variables);
        }
        flowMachine.start();
    }

    @Override
    public void initialize(String stateRepresentation, Map<Object, Object> variables) {
        S state  = stateConverter.convert(stateRepresentation);
        List<? extends StateMachineAccess<S, E>> withAllRegions = flowMachine.getStateMachineAccessor().withAllRegions();
        for (StateMachineAccess<S, E> access : withAllRegions) {
            access.resetStateMachine(new DefaultStateMachineContext<>(state, null, null, null));
        }
        if (variables != null) {
            flowMachine.getExtendedState().getVariables().putAll(variables);
        }
        flowMachine.start();
    }

    @Override
    public void stop() {
        flowMachine.stop();
    }

    @Override
    public S getCurrentState() {
        return flowMachine.getState().getId();
    }

    @Override
    public Map<Object, Object> getVariables() {
        return flowMachine.getExtendedState().getVariables();
    }

    @Override
    public Class<? extends FlowConfiguration<E>> getFlowConfigClass() {
        return flowConfigClass;
    }

    @Override
    public boolean sendEvent(FlowEventContext flowEventContext) {
        return flowMachine.sendEvent(messageFactory.createMessage(flowEventContext, eventConverter.convert(flowEventContext.getKey())));
    }

    @Override
    public String getFlowId() {
        return flowId;
    }

    @Override
    public boolean isFlowFailed() {
        return flowFailed;
    }

    @Override
    public void setFlowFailed(Exception exception) {
        flowFailed = true;
        flowEventListener.setException(exception);
    }
}
