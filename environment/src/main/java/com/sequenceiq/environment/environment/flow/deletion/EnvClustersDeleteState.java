package com.sequenceiq.environment.environment.flow.deletion;

import com.sequenceiq.flow.core.FlowState;

public enum EnvClustersDeleteState implements FlowState {

    ENV_CLUSTERS_DELETE_INIT_STATE,
    DATAHUB_CLUSTERS_DELETE_STARTED_STATE,
    EXPERIENCE_DELETE_STARTED_STATE,
    DATALAKE_CLUSTERS_DELETE_STARTED_STATE,
    ENV_CLUSTERS_DELETE_FINISHED_STATE,
    ENV_CLUSTERS_DELETE_FAILED_STATE,
    ENV_CLUSTERS_DELETE_FINAL_STATE

}
