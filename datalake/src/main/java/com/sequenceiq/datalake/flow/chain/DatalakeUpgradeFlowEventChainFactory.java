package com.sequenceiq.datalake.flow.chain;

import static com.sequenceiq.datalake.flow.datalake.upgrade.DatalakeUpgradeEvent.DATALAKE_UPGRADE_EVENT;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupEvent.DATALAKE_TRIGGER_BACKUP_EVENT;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.flow.datalake.upgrade.event.DatalakeUpgradeFlowChainStartEvent;
import com.sequenceiq.datalake.flow.datalake.upgrade.event.DatalakeUpgradeStartEvent;
import com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupFailureReason;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeTriggerBackupEvent;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class DatalakeUpgradeFlowEventChainFactory implements FlowEventChainFactory<DatalakeUpgradeFlowChainStartEvent> {
    @Override
    public String initEvent() {
        return DatalakeUpgradeFlowChainStartEvent.DATALAKE_UPGRADE_FLOW_CHAIN_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(DatalakeUpgradeFlowChainStartEvent event) {
        Queue<Selectable> chain = new ConcurrentLinkedQueue<>();
        chain.add(new DatalakeTriggerBackupEvent(DATALAKE_TRIGGER_BACKUP_EVENT.event(),
                event.getResourceId(), event.getUserId(), event.getBackupLocation(), "",
                DatalakeBackupFailureReason.BACKUP_ON_UPGRADE, event.accepted()));
        chain.add(new DatalakeUpgradeStartEvent(DATALAKE_UPGRADE_EVENT.event(), event.getResourceId(), event.getUserId(),
                event.getImageId(), event.getReplaceVms()));
        return new FlowTriggerEventQueue(getName(), event, chain);
    }
}
