package com.sequenceiq.flow.core.chain.config;

import java.util.Queue;

import com.sequenceiq.cloudbreak.common.event.Selectable;

public class FlowTriggerEventQueue {

    private final String flowChainName;

    private final Queue<Selectable> queue;

    public FlowTriggerEventQueue(String flowChainName, Queue<Selectable> queue) {
        this.flowChainName = flowChainName;
        this.queue = queue;
    }

    public String getFlowChainName() {
        return flowChainName;
    }

    public Queue<Selectable> getQueue() {
        return queue;
    }
}
