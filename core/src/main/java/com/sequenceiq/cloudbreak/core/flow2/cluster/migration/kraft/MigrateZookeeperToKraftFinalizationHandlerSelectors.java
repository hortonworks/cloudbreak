package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft;

import com.sequenceiq.flow.core.FlowEvent;

public enum MigrateZookeeperToKraftFinalizationHandlerSelectors implements FlowEvent {

    FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_VALIDATION_EVENT,
    FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;

    @Override
    public String event() {
        return name();
    }

}