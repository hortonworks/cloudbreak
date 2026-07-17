package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class MigrateZookeeperToKraftEvent extends StackEvent {

    private final boolean brokerRollingRestartNeeded;

    @JsonCreator
    public MigrateZookeeperToKraftEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("brokerRollingRestartNeeded") boolean brokerRollingRestartNeeded) {
        super(selector, resourceId);
        this.brokerRollingRestartNeeded = brokerRollingRestartNeeded;
    }

    @Override
    public String toString() {
        return "MigrateZookeeperToKraftEvent{" +
                "selector='" + selector() + '\'' +
                ", brokerRollingRestartNeeded=" + brokerRollingRestartNeeded +
                '}' + super.toString();
    }

    public boolean isBrokerRollingRestartNeeded() {
        return brokerRollingRestartNeeded;
    }
}
