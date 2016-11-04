package com.sequenceiq.cloudbreak.core.flow2.chain;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.FlowTriggers;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

@Component
public class UpgradeFlowEventChainFactory implements FlowEventChainFactory<StackEvent> {
    @Override
    public String initEvent() {
        return FlowChainTriggers.CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public Queue<Selectable> createFlowTriggerEventQueue(StackEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new StackEvent(FlowTriggers.CLUSTER_STOP_TRIGGER_EVENT, event.getStackId()));
        flowEventChain.add(new StackEvent(FlowTriggers.CLUSTER_UPGRADE_TRIGGER_EVENT, event.getStackId()));
        flowEventChain.add(new StackEvent(FlowTriggers.CLUSTER_START_TRIGGER_EVENT, event.getStackId()));
        return flowEventChain;
    }
}
