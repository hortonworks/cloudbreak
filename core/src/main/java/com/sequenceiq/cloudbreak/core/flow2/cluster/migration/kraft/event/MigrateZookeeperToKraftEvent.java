package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class MigrateZookeeperToKraftEvent extends StackEvent {

    private final boolean staleConfigsOnly;

    @JsonCreator
    public MigrateZookeeperToKraftEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("staleConfigsOnly") boolean staleConfigsOnly) {
        super(selector, resourceId);
        this.staleConfigsOnly = staleConfigsOnly;
    }

    @Override
    public String toString() {
        return "MigrateZookeeperToKraftEvent{" +
                "selector='" + selector() + '\'' +
                ", staleConfigsOnly=" + staleConfigsOnly +
                '}' + super.toString();
    }

    public boolean isStaleConfigsOnly() {
        return staleConfigsOnly;
    }
}
