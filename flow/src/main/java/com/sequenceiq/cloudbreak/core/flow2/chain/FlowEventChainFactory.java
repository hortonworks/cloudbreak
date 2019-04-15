package com.sequenceiq.cloudbreak.core.flow2.chain;

import java.util.Queue;

import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.cloud.event.Selectable;

public interface FlowEventChainFactory<P extends Payload> {
    String initEvent();

    Queue<Selectable> createFlowTriggerEventQueue(P event);
}
