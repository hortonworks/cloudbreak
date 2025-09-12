package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft;

import com.sequenceiq.flow.core.FlowEvent;

public enum MigrateZookeeperToKraftStateSelectors implements FlowEvent {

    START_MIGRATE_ZOOKEEPER_TO_KRAFT_INIT_EVENT,
    START_MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT,
    START_MIGRATE_ZOOKEEPER_TO_KRAFT_UPSCALE_KRAFT_NODES_EVENT,
    START_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT,
    FINISH_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT,
    FINALIZE_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT,
    FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT,
    HANDLED_FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT;

    @Override
    public String event() {
        return name();
    }
}

