package com.sequenceiq.flow.core.chain.config;

import java.util.Queue;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.event.Selectable;

public class FlowTriggerEventQueue {

    private String parentFlowChainId;

    private final String flowChainName;

    private final Payload triggerEvent;

    private final Queue<Selectable> queue;

    public FlowTriggerEventQueue(String flowChainName, Payload triggerEvent, Queue<Selectable> queue) {
        this.flowChainName = flowChainName;
        this.triggerEvent = triggerEvent;
        this.queue = queue;
    }

    public String getParentFlowChainId() {
        return parentFlowChainId;
    }

    public void setParentFlowChainId(String parentFlowChainId) {
        this.parentFlowChainId = parentFlowChainId;
    }

    public String getFlowChainName() {
        return flowChainName;
    }

    public Payload getTriggerEvent() {
        return triggerEvent;
    }

    public Queue<Selectable> getQueue() {
        return queue;
    }
}
