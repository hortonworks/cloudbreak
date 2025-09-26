package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum MigrateZookeeperToKraftConfigurationState implements FlowState {
    INIT_STATE,
    MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_STATE,
    MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_FINISHED_STATE,
    MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
