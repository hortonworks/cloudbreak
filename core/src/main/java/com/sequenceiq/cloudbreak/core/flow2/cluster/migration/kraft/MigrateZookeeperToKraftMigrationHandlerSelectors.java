package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft;

import com.sequenceiq.flow.core.FlowEvent;

public enum MigrateZookeeperToKraftMigrationHandlerSelectors implements FlowEvent {

    MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT,
    RESTART_KAFKA_BROKER_NODES_EVENT,
    RESTART_KAFKA_CONNECT_NODES_EVENT,
    MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT;

    @Override
    public String event() {
        return name();
    }

}
