package com.sequenceiq.cloudbreak.core.flow2;

import org.springframework.statemachine.listener.StateMachineListener;

public interface FlowEventListener<S, E> extends StateMachineListener<S, E> {
    void setException(Exception exception);
}
