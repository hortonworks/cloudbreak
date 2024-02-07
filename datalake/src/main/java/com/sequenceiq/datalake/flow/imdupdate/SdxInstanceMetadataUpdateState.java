package com.sequenceiq.datalake.flow.imdupdate;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum SdxInstanceMetadataUpdateState implements FlowState {

    INIT_STATE,
    SDX_IMD_UPDATE_STATE,
    SDX_IMD_UPDATE_WAIT_STATE,
    SDX_IMD_UPDATE_FINISHED_STATE,
    SDX_IMD_UPDATE_FAILED_STATE,
    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    SdxInstanceMetadataUpdateState() {
    }

    SdxInstanceMetadataUpdateState(Class<? extends DefaultRestartAction> restartAction) {
        this.restartAction = restartAction;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }

}
