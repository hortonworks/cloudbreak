package com.sequenceiq.datalake.flow.loadbalancer.dns.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class UpdateLoadBalancerDNSFailedEvent extends SdxFailedEvent {
    @JsonCreator
    public UpdateLoadBalancerDNSFailedEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("exception") Exception e) {
        super(sdxId, userId, e);
    }

    public static UpdateLoadBalancerDNSFailedEvent from(SdxEvent event, Exception e) {
        return new UpdateLoadBalancerDNSFailedEvent(event.getResourceId(), event.getUserId(), e);
    }
}
