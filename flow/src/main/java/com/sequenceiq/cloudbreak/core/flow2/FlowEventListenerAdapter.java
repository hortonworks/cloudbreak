package com.sequenceiq.cloudbreak.core.flow2;

import org.springframework.context.annotation.Scope;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class FlowEventListenerAdapter<S, E> extends StateMachineListenerAdapter<S, E> implements FlowEventListener<S, E> {
    public FlowEventListenerAdapter(S initState, S finalState, String flowType, String flowId, Long stackId) {
    }

    @Override
    public void setException(Exception exception) {
    }
}
