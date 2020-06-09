package com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.backup;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup.DatabaseBackupEvent.DATABASE_BACKUP_FINISHED_EVENT;

public class DatabaseBackupSuccess extends StackEvent {

    public DatabaseBackupSuccess(Long stackId) {
        super(stackId);
    }

    @Override
    public String selector() {
        return DATABASE_BACKUP_FINISHED_EVENT.event();
    }
}
