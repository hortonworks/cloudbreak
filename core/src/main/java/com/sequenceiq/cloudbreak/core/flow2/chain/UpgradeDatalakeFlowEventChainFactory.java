package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeEvent.CLUSTER_UPGRADE_INIT_EVENT;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.event.DatalakeClusterUpgradeTriggerEvent;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;

@Component
public class UpgradeDatalakeFlowEventChainFactory implements FlowEventChainFactory<DatalakeClusterUpgradeTriggerEvent> {
    @Override
    public String initEvent() {
        return FlowChainTriggers.DATALAKE_CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public Queue<Selectable> createFlowTriggerEventQueue(DatalakeClusterUpgradeTriggerEvent event) {
        Queue<Selectable> chain = new ConcurrentLinkedQueue<>();
        chain.add(new DatalakeClusterUpgradeTriggerEvent(
                CLUSTER_UPGRADE_INIT_EVENT.event(), event.getResourceId(), event.accepted(), event.getCurrentImage(), event.getTargetImage()));
        return chain;
    }
}
