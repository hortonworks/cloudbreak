package com.sequenceiq.datalake.flow.chain;


import static com.sequenceiq.datalake.flow.datalake.upgrade.preparation.DatalakeUpgradePreparationEvent.DATALAKE_UPGRADE_PREPARATION_TRIGGER_EVENT;
import static com.sequenceiq.datalake.flow.dr.validation.DatalakeBackupValidationEvent.DATALAKE_TRIGGER_BACKUP_VALIDATION_EVENT;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.flow.datalake.upgrade.event.DatalakeUpgradePreparationFlowChainStartEvent;
import com.sequenceiq.datalake.flow.datalake.upgrade.preparation.event.DatalakeUpgradePreparationStartEvent;
import com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupFailureReason;
import com.sequenceiq.datalake.flow.dr.validation.event.DatalakeTriggerBackupValidationEvent;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class DatalakeUpgradePreparationFlowEventChainFactory implements FlowEventChainFactory<DatalakeUpgradePreparationFlowChainStartEvent> {
    @Override
    public String initEvent() {
        return DatalakeUpgradePreparationFlowChainStartEvent.DATALAKE_UPGRADE_PREPARATION_FLOW_CHAIN_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(DatalakeUpgradePreparationFlowChainStartEvent event) {
        Queue<Selectable> chain = new ConcurrentLinkedQueue<>();
        chain.add(new DatalakeTriggerBackupValidationEvent(DATALAKE_TRIGGER_BACKUP_VALIDATION_EVENT.event(),
                event.getResourceId(), event.getUserId(), event.getBackupLocation(),
                DatalakeBackupFailureReason.BACKUP_ON_UPGRADE, event.accepted()));
        chain.add(new DatalakeUpgradePreparationStartEvent(DATALAKE_UPGRADE_PREPARATION_TRIGGER_EVENT.event(), event.getResourceId(),
                        event.getUserId(), event.getImageId()));
        return new FlowTriggerEventQueue(getName(), event, chain);
    }
}
