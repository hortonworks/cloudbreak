package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.START_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_VALIDATION_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class MigrateZookeeperToKraftConfigurationTriggerEvent extends StackEvent {

    private final boolean kraftInstallNeeded;

    @JsonCreator
    public MigrateZookeeperToKraftConfigurationTriggerEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("kraftInstallNeeded") boolean kraftInstallNeeded,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(START_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_VALIDATION_EVENT.event(), resourceId, accepted);
        this.kraftInstallNeeded = kraftInstallNeeded;
    }

    @Override
    public String toString() {
        return "MigrateZookeeperToKraftConfigurationTriggerEvent{" +
                "kraftInstallNeeded=" + kraftInstallNeeded +
                "} " + super.toString();
    }

    public boolean isKraftInstallNeeded() {
        return kraftInstallNeeded;
    }

}