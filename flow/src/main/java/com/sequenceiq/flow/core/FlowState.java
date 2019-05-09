package com.sequenceiq.flow.core;

import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public interface FlowState {
    default Class<? extends AbstractAction<?, ?, ?, ?>> action() {
        return null;
    }

    default Class<? extends RestartAction> restartAction() {
        return DefaultRestartAction.class;
    }

    String name();
}
