package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class MigrateZookeeperToKraftEvent extends StackEvent {

    private final boolean staleConfigsOnly;

    private final boolean kraftHostGroupPresent;

    @JsonCreator
    public MigrateZookeeperToKraftEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("staleConfigsOnly") boolean staleConfigsOnly,
            @JsonProperty("kraftHostGroupPresent") boolean kraftHostGroupPresent) {
        super(selector, resourceId);
        this.staleConfigsOnly = staleConfigsOnly;
        this.kraftHostGroupPresent = kraftHostGroupPresent;
    }

    @Override
    public String toString() {
        return "MigrateZookeeperToKraftEvent{" +
                "selector='" + selector() + '\'' +
                ", staleConfigsOnly=" + staleConfigsOnly +
                ", kraftHostGroupPresent=" + kraftHostGroupPresent +
                '}' + super.toString();
    }

    public boolean isStaleConfigsOnly() {
        return staleConfigsOnly;
    }

    public boolean isKraftHostGroupPresent() {
        return kraftHostGroupPresent;
    }
}
