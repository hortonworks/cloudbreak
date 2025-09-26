package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class MigrateZookeeperToKraftValidationEvent extends StackEvent {

    @JsonCreator
    public MigrateZookeeperToKraftValidationEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId) {
        super(selector, resourceId);
    }

    @Override
    public String toString() {
        return "MigrateZookeeperToKraftValidationEvent{" +
                "selector='" + selector() + '\'' +
                '}' + super.toString();
    }
}
