package com.sequenceiq.environment.environment.flow.deletion;

import com.sequenceiq.environment.environment.flow.EnvironmentFillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum EnvClustersDeleteState implements FlowState {

    ENV_CLUSTERS_DELETE_INIT_STATE,
    DATAHUB_CLUSTERS_DELETE_STARTED_STATE,
    EXPERIENCE_DELETE_STARTED_STATE,
    DATALAKE_CLUSTERS_DELETE_STARTED_STATE,
    ENV_CLUSTERS_DELETE_FINISHED_STATE,
    ENV_CLUSTERS_DELETE_FAILED_STATE,
    ENV_CLUSTERS_DELETE_FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return EnvironmentFillInMemoryStateStoreRestartAction.class;
    }
}
