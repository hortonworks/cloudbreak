package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft;

import com.sequenceiq.flow.core.FlowEvent;

public enum MigrateZookeeperToKraftFinalizationStateSelectors implements FlowEvent {

    START_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT,
    FINISH_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT,
    FINALIZE_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT,
    FAILED_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT,
    HANDLED_FAILED_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;

    @Override
    public String event() {
        return name();
    }
}