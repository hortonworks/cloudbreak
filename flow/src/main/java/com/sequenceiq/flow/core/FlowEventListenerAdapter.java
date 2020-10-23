package com.sequenceiq.flow.core;

import org.springframework.context.annotation.Scope;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class FlowEventListenerAdapter<S, E> extends StateMachineListenerAdapter<S, E> implements FlowEventListener<S, E> {

    public FlowEventListenerAdapter(S initState, S finalState, String flowChainType, String flowType,
            String flowChainId, String flowId, Long resourceId) {
    }

    @Override
    public void setException(Exception exception) {
    }
}
