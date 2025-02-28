package com.sequenceiq.datalake.flow.certrotation;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum RotateCertificateState implements FlowState {

    INIT_STATE,
    ROTATE_CERTIFICATE_STACK_STATE,
    ROTATE_CERTIFICATE_FINISHED_STATE,
    ROTATE_CERTIFICATE_FAILED_STATE,
    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    RotateCertificateState() {
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }

}
