package com.sequenceiq.cloudbreak.core.flow2;

public interface FlowState {
    default Class<? extends AbstractAction> action() {
        return null;
    }

    default Class<? extends RestartAction> restartAction() {
        return DefaultRestartAction.class;
    }

    String name();
}
