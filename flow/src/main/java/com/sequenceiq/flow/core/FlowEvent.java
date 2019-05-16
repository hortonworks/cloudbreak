package com.sequenceiq.flow.core;

public interface FlowEvent {
    String name();

    String event();

    default String selector() {
        return name();
    }
}
