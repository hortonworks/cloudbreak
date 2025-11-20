package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum MigrateZookeeperToKraftFinalizationState implements FlowState {
    INIT_STATE,
    FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_VALIDATION_STATE,
    FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_STATE,
    FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_FINISHED_STATE,
    FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}