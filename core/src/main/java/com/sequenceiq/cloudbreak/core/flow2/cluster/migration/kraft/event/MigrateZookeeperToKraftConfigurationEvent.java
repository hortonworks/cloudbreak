package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class MigrateZookeeperToKraftConfigurationEvent extends StackEvent {

    private final boolean kraftInstallNeeded;

    @JsonCreator
    public MigrateZookeeperToKraftConfigurationEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("kraftInstallNeeded")  boolean kraftInstallNeeded) {
        super(selector, resourceId);
        this.kraftInstallNeeded = kraftInstallNeeded;
    }

    @Override
    public String toString() {
        return "MigrateZookeeperToKraftConfigurationEvent{" +
                "selector='" + selector() + '\'' +
                "kraftInstallNeeded='" + kraftInstallNeeded + '\'' +
                '}' + super.toString();
    }

    public boolean isKraftInstallNeeded() {
        return kraftInstallNeeded;
    }

}
