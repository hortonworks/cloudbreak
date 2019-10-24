package com.sequenceiq.freeipa.flow.stack.termination.event.clusterproxy;

import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.stack.termination.event.TerminationEvent;

public class ClusterProxyDeregistrationRequest extends TerminationEvent {
    public ClusterProxyDeregistrationRequest(Long stackId, Boolean forced) {
        super(EventSelectorUtil.selector(ClusterProxyDeregistrationRequest.class), stackId, forced);
    }
}
