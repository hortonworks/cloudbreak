package com.sequenceiq.cloudbreak.core.flow2.chain;

import java.util.Queue;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.event.Selectable;

public interface FlowEventChainFactory<P extends Payload> {
    String initEvent();

    Queue<Selectable> createFlowTriggerEventQueue(P event);
}
