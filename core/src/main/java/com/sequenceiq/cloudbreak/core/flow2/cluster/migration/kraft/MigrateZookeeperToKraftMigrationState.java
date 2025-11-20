package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum MigrateZookeeperToKraftMigrationState implements FlowState {
    INIT_STATE,
    MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_STATE,
    RESTART_KAFKA_BROKER_NODES_STATE,
    RESTART_KAFKA_CONNECT_NODES_STATE,
    MIGRATE_ZOOKEEPER_TO_KRAFT_STATE,
    MIGRATE_ZOOKEEPER_TO_KRAFT_FINISHED_STATE,
    MIGRATE_ZOOKEEPER_TO_KRAFT_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
