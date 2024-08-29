package com.sequenceiq.datalake.flow.verticalscale.rootvolume;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum DatalakeRootVolumeUpdateState implements FlowState {

    INIT_STATE,
    DATALAKE_ROOT_VOLUME_UPDATE_STATE,
    DATALAKE_ROOT_VOLUME_UPDATE_FINISHED_STATE,
    DATALAKE_ROOT_VOLUME_UPDATE_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
