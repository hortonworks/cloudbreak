package com.sequenceiq.cloudbreak.core.flow2.stack.downscale;

import com.sequenceiq.cloudbreak.core.flow2.FlowState;
import com.sequenceiq.cloudbreak.core.flow2.RestartAction;
import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;

public enum StackDownscaleState implements FlowState {
    INIT_STATE,
    DOWNSCALE_FAILED_STATE,
    DOWNSCALE_COLLECT_RESOURCES_STATE,
    DOWNSCALE_STATE,
    DOWNSCALE_FINISHED_STATE,
    FINAL_STATE;

    private final Class<? extends RestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
