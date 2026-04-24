package com.sequenceiq.datalake.flow.datalake.restartservices;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum DatalakeRestartServicesFlowState implements FlowState {

    INIT_STATE,
    DATALAKE_RESTART_SERVICES_START_STATE,
    DATALAKE_RESTART_SERVICES_IN_PROGRESS_STATE,
    DATALAKE_RESTART_SERVICES_FINISHED_STATE,
    DATALAKE_RESTART_SERVICES_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
