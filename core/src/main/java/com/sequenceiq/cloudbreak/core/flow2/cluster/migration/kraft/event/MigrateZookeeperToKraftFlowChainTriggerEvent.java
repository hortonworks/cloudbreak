package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class MigrateZookeeperToKraftFlowChainTriggerEvent extends StackEvent {

    public MigrateZookeeperToKraftFlowChainTriggerEvent(
            String selector,
            Long resourceId) {
        super(selector, resourceId);
    }

    @JsonCreator
    public MigrateZookeeperToKraftFlowChainTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(selector, resourceId, accepted);
    }

    @Override
    public String toString() {
        return "MigrateZookeeperToKraftFlowChainTriggerEvent{" +
                "} " + super.toString();
    }
}
