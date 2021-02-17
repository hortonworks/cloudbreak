package com.sequenceiq.flow.core.chain;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

public interface FlowEventChainFactory<P extends Payload> {

    String initEvent();

    FlowTriggerEventQueue createFlowTriggerEventQueue(P event);

    default String getName() {
        return getClass().getSimpleName();
    }
}
