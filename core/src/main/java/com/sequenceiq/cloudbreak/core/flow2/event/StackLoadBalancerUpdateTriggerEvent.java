package com.sequenceiq.cloudbreak.core.flow2.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class StackLoadBalancerUpdateTriggerEvent extends StackEvent {

    public StackLoadBalancerUpdateTriggerEvent(Long stackId) {
        super(stackId);
    }

    public StackLoadBalancerUpdateTriggerEvent(String selector, Long stackId) {
        super(selector, stackId);
    }

    @JsonCreator
    public StackLoadBalancerUpdateTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
    }
}
