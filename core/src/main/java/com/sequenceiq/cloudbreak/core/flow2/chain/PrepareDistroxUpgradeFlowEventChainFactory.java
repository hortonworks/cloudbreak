package com.sequenceiq.cloudbreak.core.flow2.chain;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradePreparationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.DistroXUpgradePreparationChainTriggerEvent;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class PrepareDistroxUpgradeFlowEventChainFactory implements FlowEventChainFactory<DistroXUpgradePreparationChainTriggerEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrepareDistroxUpgradeFlowEventChainFactory.class);

    @Override
    public String initEvent() {
        return FlowChainTriggers.DISTROX_CLUSTER_UPGRADE_PREPARATION_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(DistroXUpgradePreparationChainTriggerEvent event) {
        LOGGER.debug("Creating flow trigger event queue for distrox upgrade preparation with event {}", event);
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(createUpgradeValidationTriggerEvent(event));
        flowEventChain.add(createClusterUpgradePreparationTriggerEvent(event));
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }

    private ClusterUpgradeValidationTriggerEvent createUpgradeValidationTriggerEvent(DistroXUpgradePreparationChainTriggerEvent event) {
        return new ClusterUpgradeValidationTriggerEvent(event.getResourceId(), event.accepted(), event.getImageChangeDto().getImageId(),
                event.isLockComponents());
    }

    private ClusterUpgradePreparationTriggerEvent createClusterUpgradePreparationTriggerEvent(DistroXUpgradePreparationChainTriggerEvent event) {
        return new ClusterUpgradePreparationTriggerEvent(event.getResourceId(), event.accepted(), event.getImageChangeDto());
    }
}
