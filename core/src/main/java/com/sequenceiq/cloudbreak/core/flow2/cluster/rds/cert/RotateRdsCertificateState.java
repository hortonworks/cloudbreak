package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum RotateRdsCertificateState implements FlowState {
    INIT_STATE,
    ROTATE_RDS_CERTIFICATE_FAILED_STATE,
    ROTATE_RDS_CERTIFICATE_CHECK_PREREQUISITES_STATE,
    ROTATE_RDS_SETUP_TLS_STATE,
    ROTATE_RDS_CERTIFICATE_GET_LATEST_STATE,
    ROTATE_RDS_CERTIFICATE_UPDATE_TO_LATEST_STATE,
    ROTATE_RDS_CERTIFICATE_RESTART_CM_STATE,
    ROTATE_RDS_CERTIFICATE_ROLLING_RESTART_STATE,
    ROTATE_RDS_CERTIFICATE_ON_PROVIDER_STATE,
    ROTATE_RDS_CERTIFICATE_FINISHED_STATE,
    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    RotateRdsCertificateState() {

    }

    RotateRdsCertificateState(Class<? extends DefaultRestartAction> restartAction) {
        this.restartAction = restartAction;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
