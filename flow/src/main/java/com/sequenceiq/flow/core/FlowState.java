package com.sequenceiq.flow.core;

public interface FlowState {
    default Class<? extends AbstractAction<?, ?, ?, ?>> action() {
        return null;
    }

    Class<? extends RestartAction> restartAction();

    String name();

    enum FlowStateConstants implements FlowState {
        INIT_STATE;

        @Override
        public Class<? extends RestartAction> restartAction() {
            return null;
        }
    }
}
