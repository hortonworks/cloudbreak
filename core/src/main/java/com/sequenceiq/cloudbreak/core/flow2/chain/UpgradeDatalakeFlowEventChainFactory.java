package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeEvent.CLUSTER_UPGRADE_INIT_EVENT;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterUpgradeTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;

@Component
public class UpgradeDatalakeFlowEventChainFactory implements FlowEventChainFactory<ClusterUpgradeTriggerEvent> {
    @Override
    public String initEvent() {
        return FlowChainTriggers.DATALAKE_CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public Queue<Selectable> createFlowTriggerEventQueue(ClusterUpgradeTriggerEvent event) {
        Queue<Selectable> chain = new ConcurrentLinkedQueue<>();
        chain.add(new StackEvent(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), event.getResourceId(), event.accepted()));
        chain.add(new ClusterUpgradeTriggerEvent(CLUSTER_UPGRADE_INIT_EVENT.event(), event.getResourceId(), event.accepted(), event.getImageId()));
        return chain;
    }
}
