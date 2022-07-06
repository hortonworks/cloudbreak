package com.sequenceiq.freeipa.flow.stack.termination.event.clusterproxy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.stack.termination.event.TerminationEvent;

public class ClusterProxyDeregistrationFinished extends TerminationEvent {
    @JsonCreator
    public ClusterProxyDeregistrationFinished(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("forced") Boolean forced) {
        super(EventSelectorUtil.selector(ClusterProxyDeregistrationFinished.class), stackId, forced);
    }
}
