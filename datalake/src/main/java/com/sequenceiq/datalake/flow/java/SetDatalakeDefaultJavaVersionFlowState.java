package com.sequenceiq.datalake.flow.java;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum SetDatalakeDefaultJavaVersionFlowState implements FlowState {

    INIT_STATE,
    SET_DATALAKE_DEFAULT_JAVA_VERSION_STATE,
    SET_DATALAKE_DEFAULT_JAVA_VERSION_FINISED_STATE,
    FINAL_STATE,
    SET_DATALAKE_DEFAULT_JAVA_VERSION_FAILED_STATE;

    private final Class<? extends RestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }

}
