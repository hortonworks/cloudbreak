package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartEvent.CLUSTER_START_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stop.ClusterStopEvent.CLUSTER_STOP_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upgrade.ClusterUpgradeEvent.CLUSTER_UPGRADE_EVENT;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
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
        flowEventChain.add(new StackEvent(CLUSTER_STOP_EVENT.event(), event.getStackId()));
        flowEventChain.add(new StackEvent(CLUSTER_UPGRADE_EVENT.event(), event.getStackId()));
        flowEventChain.add(new StackEvent(CLUSTER_START_EVENT.event(), event.getStackId()));
        return flowEventChain;
    }
}
