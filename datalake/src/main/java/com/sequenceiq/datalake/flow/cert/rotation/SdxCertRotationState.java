package com.sequenceiq.datalake.flow.cert.rotation;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum SdxCertRotationState implements FlowState {
    INIT_STATE,
    START_CERT_ROTATION_STATE,
    CERT_ROTATION_IN_PROGRESS_STATE,
    CERT_ROTATION_FINISHED_STATE,
    CERT_ROTATION_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
