package com.sequenceiq.datalake.flow.encryptionprofile;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum SdxEnableEncryptionProfileState implements FlowState {

    INIT_STATE,
    SDX_ENABLE_ENCRYPTION_PROFILE_STATE,
    SDX_ENABLE_ENCRYPTION_PROFILE_FINISHED_STATE,
    SDX_ENABLE_ENCRYPTION_PROFILE_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
