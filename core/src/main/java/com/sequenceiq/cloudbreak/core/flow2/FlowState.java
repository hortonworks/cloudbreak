package com.sequenceiq.cloudbreak.core.flow2;

public interface FlowState<S extends FlowState, E extends FlowEvent> {
    Class<?> action();
    String name();
    S failureState();
    E failureEvent();
    void setFailureEvent(E failureEvent);
}
