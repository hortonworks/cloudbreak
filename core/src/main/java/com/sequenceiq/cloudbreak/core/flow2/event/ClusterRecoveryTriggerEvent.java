package com.sequenceiq.cloudbreak.core.flow2.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

import reactor.rx.Promise;

public class ClusterRecoveryTriggerEvent extends StackEvent {

    public ClusterRecoveryTriggerEvent(String selector, Long stackId) {
        super(selector, stackId);
    }

    @JsonCreator
    public ClusterRecoveryTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(ClusterRecoveryTriggerEvent.class, other);
    }
}
