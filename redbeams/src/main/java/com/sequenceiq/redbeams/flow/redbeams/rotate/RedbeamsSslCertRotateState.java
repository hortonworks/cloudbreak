package com.sequenceiq.redbeams.flow.redbeams.rotate;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;
import com.sequenceiq.redbeams.flow.redbeams.common.FillInMemoryStateStoreRestartAction;

public enum RedbeamsSslCertRotateState implements FlowState {
    INIT_STATE,
    REDBEAMS_SSL_CERT_ROTATE_FAILED_STATE,
    SSL_CERT_ROTATE_DATABASE_SERVER_STATE,
    REDBEAMS_SSL_CERT_ROTATE_FINISHED_STATE,
    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }

}
