package com.sequenceiq.datalake.flow.loadbalancer.dns.event;

import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class UpdateLoadBalancerDNSFailedEvent extends SdxFailedEvent {
    public UpdateLoadBalancerDNSFailedEvent(Long sdxId, String userId, Exception e) {
        super(sdxId, userId, e);
    }

    public static UpdateLoadBalancerDNSFailedEvent from(SdxEvent event, Exception e) {
        return new UpdateLoadBalancerDNSFailedEvent(event.getResourceId(), event.getUserId(), e);
    }
}
