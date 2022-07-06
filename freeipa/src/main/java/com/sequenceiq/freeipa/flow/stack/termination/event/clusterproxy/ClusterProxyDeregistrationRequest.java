package com.sequenceiq.freeipa.flow.stack.termination.event.clusterproxy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.stack.termination.event.TerminationEvent;

public class ClusterProxyDeregistrationRequest extends TerminationEvent {
    @JsonCreator
    public ClusterProxyDeregistrationRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("forced") Boolean forced) {
        super(EventSelectorUtil.selector(ClusterProxyDeregistrationRequest.class), stackId, forced);
    }
}
