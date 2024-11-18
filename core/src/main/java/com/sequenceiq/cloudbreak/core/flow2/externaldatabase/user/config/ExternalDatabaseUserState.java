package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.user.config;

import com.sequenceiq.cloudbreak.core.flow2.restart.InitializeMDCContextRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum ExternalDatabaseUserState  implements FlowState {
    INIT_STATE,
    EXECUTE_EXTERNAL_DATABASE_USER_OPERATION_STATE,
    EXTERNAL_DATABASE_USER_OPERATION_FAILED_STATE,
    EXTERNAL_DATABASE_USER_OPERATION_FINISHED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return InitializeMDCContextRestartAction.class;
    }
}
