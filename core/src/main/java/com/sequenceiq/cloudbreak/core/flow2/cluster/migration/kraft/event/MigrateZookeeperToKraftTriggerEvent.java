package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event;


import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationStateSelectors.START_MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class MigrateZookeeperToKraftTriggerEvent extends StackEvent {

    private final boolean staleConfigsOnly;

    @JsonCreator
    public MigrateZookeeperToKraftTriggerEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("staleConfigsOnly") boolean staleConfigsOnly,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(START_MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT.event(), resourceId, accepted);
        this.staleConfigsOnly = staleConfigsOnly;
    }

    @Override
    public String toString() {
        return "MigrateZookeeperToKraftTriggerEvent{" +
                "staleConfigsOnly=" + staleConfigsOnly +
                "} " + super.toString();
    }

    public boolean isStaleConfigsOnly() {
        return staleConfigsOnly;
    }
}
