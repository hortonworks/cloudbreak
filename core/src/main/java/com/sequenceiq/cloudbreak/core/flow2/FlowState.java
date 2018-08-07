package com.sequenceiq.cloudbreak.core.flow2;

import com.sequenceiq.cloudbreak.core.flow2.restart.DefaultRestartAction;

public interface FlowState {
    default Class<? extends AbstractAction<?, ?, ?, ?>> action() {
        return null;
    }

    default Class<? extends RestartAction> restartAction() {
        return DefaultRestartAction.class;
    }

    String name();
}
