package com.sequenceiq.flow.core;

public interface FlowState {
    default Class<? extends AbstractAction<?, ?, ?, ?>> action() {
        return null;
    }

    Class<? extends RestartAction> restartAction();

    String name();
}
