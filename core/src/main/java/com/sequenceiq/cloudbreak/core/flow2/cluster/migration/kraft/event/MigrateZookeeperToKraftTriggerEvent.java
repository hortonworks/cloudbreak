package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftHandlerSelectors.MIGRATE_ZOOKEEPER_TO_KRAFT_INIT_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class MigrateZookeeperToKraftTriggerEvent extends StackEvent {
    @JsonCreator
    public MigrateZookeeperToKraftTriggerEvent(
            @JsonProperty("resourceId") Long resourceId) {
        super(MIGRATE_ZOOKEEPER_TO_KRAFT_INIT_EVENT.event(), resourceId);
    }

    @Override
    public String toString() {
        return "MigrateZookeeperToKraftTriggerEvent{" +
                "} " + super.toString();
    }
}
