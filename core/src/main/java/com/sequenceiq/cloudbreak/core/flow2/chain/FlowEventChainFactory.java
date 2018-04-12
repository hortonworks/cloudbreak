package com.sequenceiq.cloudbreak.core.flow2.chain;

import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.cloud.event.Selectable;

import java.util.Queue;

public interface FlowEventChainFactory<P extends Payload> {
    String initEvent();

    Queue<Selectable> createFlowTriggerEventQueue(P event);
}
