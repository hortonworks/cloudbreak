package com.sequenceiq.cloudbreak.core.flow2.cluster.java;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum SetDefaultJavaVersionFlowState implements FlowState {

    INIT_STATE,
    SET_DEFAULT_JAVA_VERSION_STATE,
    SET_DEFAULT_JAVA_VERSION_FINISED_STATE,
    FINAL_STATE,
    SET_DEFAULT_JAVA_VERSION_FAILED_STATE;

    private final Class<? extends RestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
