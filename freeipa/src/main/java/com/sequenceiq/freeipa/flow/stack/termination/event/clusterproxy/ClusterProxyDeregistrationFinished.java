package com.sequenceiq.freeipa.flow.stack.termination.event.clusterproxy;

import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.stack.termination.event.TerminationEvent;

public class ClusterProxyDeregistrationFinished extends TerminationEvent {
    public ClusterProxyDeregistrationFinished(Long stackId, Boolean forced) {
        super(EventSelectorUtil.selector(ClusterProxyDeregistrationFinished.class), stackId, forced);
    }
}
