package com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.handler.removeloadbalancer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class RemoveLoadBalancerResult extends StackEvent implements FlowPayload {

    @JsonCreator
    public RemoveLoadBalancerResult(@JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }

    @Override
    public String toString() {
        return "RemoveLoadBalancerResult{} " + super.toString();
    }
}
