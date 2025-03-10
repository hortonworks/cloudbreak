package com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class SkuMigrationTriggerEvent extends StackEvent {

    private final boolean force;

    public SkuMigrationTriggerEvent(String selector, Long stackId, boolean force) {
        super(selector, stackId);
        this.force = force;
    }

    @JsonCreator
    public SkuMigrationTriggerEvent(@JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("force") boolean force,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
        this.force = force;
    }

    public boolean isForce() {
        return force;
    }

    @Override
    public String toString() {
        return "LoadBalancerMigrationRemovalTriggerEvent{" +
                "force=" + force +
                "} " + super.toString();
    }
}
