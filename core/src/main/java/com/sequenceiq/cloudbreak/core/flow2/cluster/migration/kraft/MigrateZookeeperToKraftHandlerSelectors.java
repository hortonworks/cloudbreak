package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft;

import com.sequenceiq.flow.core.FlowEvent;

public enum MigrateZookeeperToKraftHandlerSelectors implements FlowEvent {

    MIGRATE_ZOOKEEPER_TO_KRAFT_INIT_EVENT,
    MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT,
    MIGRATE_ZOOKEEPER_TO_KRAFT_UPSCALE_KRAFT_NODES_EVENT,
    MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT;

    @Override
    public String event() {
        return name();
    }

}
