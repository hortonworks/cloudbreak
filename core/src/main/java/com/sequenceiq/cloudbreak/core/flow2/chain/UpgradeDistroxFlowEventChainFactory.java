package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeEvent.CLUSTER_UPGRADE_INIT_EVENT;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterUpgradeTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.DistroxUpgradeTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackImageUpdateTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;

@Component
public class UpgradeDistroxFlowEventChainFactory implements FlowEventChainFactory<DistroxUpgradeTriggerEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeDistroxFlowEventChainFactory.class);

    @Override
    public String initEvent() {
        return FlowChainTriggers.DISTROX_CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public Queue<Selectable> createFlowTriggerEventQueue(DistroxUpgradeTriggerEvent event) {
        Queue<Selectable> chain = new ConcurrentLinkedQueue<>();
        chain.add(new StackEvent(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), event.getResourceId(), event.accepted()));
        chain.add(new ClusterUpgradeTriggerEvent(CLUSTER_UPGRADE_INIT_EVENT.event(), event.getResourceId(), event.accepted(),
                event.getImageChangeDto().getImageId()));
        chain.add(new StackImageUpdateTriggerEvent(FlowChainTriggers.STACK_IMAGE_UPDATE_TRIGGER_EVENT, event.getImageChangeDto()));
        if (event.isReplaceVms()) {
            // TODO
            LOGGER.warn("Replace VMs for DistroX is not implemented!");
        }
        return chain;
    }
}
