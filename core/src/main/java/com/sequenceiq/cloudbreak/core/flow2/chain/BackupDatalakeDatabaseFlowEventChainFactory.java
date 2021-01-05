package com.sequenceiq.cloudbreak.core.flow2.chain;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.DatabaseBackupTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import org.springframework.stereotype.Component;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup.DatabaseBackupEvent.DATABASE_BACKUP_EVENT;

@Component
public class BackupDatalakeDatabaseFlowEventChainFactory implements FlowEventChainFactory<DatabaseBackupTriggerEvent> {
    @Override
    public String initEvent() {
        return FlowChainTriggers.DATALAKE_DATABASE_BACKUP_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public Queue<Selectable> createFlowTriggerEventQueue(DatabaseBackupTriggerEvent event) {
        Queue<Selectable> chain = new ConcurrentLinkedQueue<>();
        chain.add(new StackEvent(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), event.getResourceId(), event.accepted()));
        chain.add(new DatabaseBackupTriggerEvent(DATABASE_BACKUP_EVENT.event(), event.getResourceId(),
                event.getBackupLocation(), event.getBackupId()));
        return chain;
    }
}
