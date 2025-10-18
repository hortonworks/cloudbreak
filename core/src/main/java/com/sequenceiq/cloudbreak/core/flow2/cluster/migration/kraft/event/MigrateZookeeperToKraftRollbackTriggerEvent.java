package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftRollbackStateSelectors.START_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class MigrateZookeeperToKraftRollbackTriggerEvent extends StackEvent {
    @JsonCreator
    public MigrateZookeeperToKraftRollbackTriggerEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(START_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT.event(), resourceId, accepted);
    }

    @Override
    public String toString() {
        return "MigrateZookeeperToKraftRollbackTriggerEvent{" +
                "} " + super.toString();
    }
}