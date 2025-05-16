package com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer;

import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.UPSCALE_UPDATE_LOAD_BALANCERS_FAILURE_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class UpscaleUpdateLoadBalancersFailed extends StackFailureEvent {

    @JsonCreator
    public UpscaleUpdateLoadBalancersFailed(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception) {
        super(UPSCALE_UPDATE_LOAD_BALANCERS_FAILURE_EVENT.selector(), stackId, exception);
    }
}
