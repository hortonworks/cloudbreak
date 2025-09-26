package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft;

import com.sequenceiq.flow.core.FlowEvent;

public enum MigrateZookeeperToKraftConfigurationStateSelectors implements FlowEvent {

    START_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT,
    FINISH_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT,
    FINALIZE_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT,
    FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT,
    HANDLED_FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT;

    @Override
    public String event() {
        return name();
    }
}
