package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft;

import com.sequenceiq.flow.core.FlowEvent;

public enum MigrateZookeeperToKraftConfigurationHandlerSelectors implements FlowEvent {

    MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_VALIDATION_EVENT,
    MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT;

    @Override
    public String event() {
        return name();
    }
}
