package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft;

import com.sequenceiq.flow.core.FlowEvent;

public enum MigrateZookeeperToKraftRollbackStateSelectors implements FlowEvent {

    START_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_VALIDATION_EVENT,
    START_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT,
    FINISH_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT,
    FINALIZE_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT,
    FAILED_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT,
    HANDLED_FAILED_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;

    @Override
    public String event() {
        return name();
    }
}