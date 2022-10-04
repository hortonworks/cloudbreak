package com.sequenceiq.cloudbreak.core.flow2.chain;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.UpgradeRdsEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation.ValidateRdsUpgradeEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.RdsUpgradeChainTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsTriggerRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeTriggerRequest;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class UpgradeRdsFlowEventChainFactory implements FlowEventChainFactory<RdsUpgradeChainTriggerEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeRdsFlowEventChainFactory.class);

    @Override
    public String initEvent() {
        return FlowChainTriggers.UPGRADE_RDS_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(RdsUpgradeChainTriggerEvent event) {
        LOGGER.debug("Creating flow trigger event queue for RDS upgrade with event {}", event);
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new StackEvent(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), event.getResourceId(), event.accepted()));
        flowEventChain.add(new ValidateRdsUpgradeTriggerRequest(ValidateRdsUpgradeEvent.VALIDATE_RDS_UPGRADE_EVENT.event(),
                event.getResourceId(), event.getVersion(), event.accepted()));
        flowEventChain.add(new UpgradeRdsTriggerRequest(UpgradeRdsEvent.UPGRADE_RDS_EVENT.event(),
                event.getResourceId(), event.getVersion(), event.getBackupLocation()));
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }
}
