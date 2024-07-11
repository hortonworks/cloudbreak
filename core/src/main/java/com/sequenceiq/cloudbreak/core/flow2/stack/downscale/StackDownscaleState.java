package com.sequenceiq.cloudbreak.core.flow2.stack.downscale;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum StackDownscaleState implements FlowState {
    INIT_STATE,
    DOWNSCALE_REMOVE_USERDATA_SECRETS_STATE,
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
