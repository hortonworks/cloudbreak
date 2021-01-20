package com.sequenceiq.datalake.flow.dr.backup.event;

import com.sequenceiq.datalake.entity.operation.SdxOperation;
import com.sequenceiq.datalake.entity.operation.SdxOperationType;
import com.sequenceiq.datalake.flow.dr.event.DatalakeDatabaseDrStartBaseEvent;

import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupEvent.DATALAKE_DATABASE_BACKUP_EVENT;

public class DatalakeDatabaseBackupStartEvent extends DatalakeDatabaseDrStartBaseEvent {

    private final String backupId;

    private final String backupLocation;

    public DatalakeDatabaseBackupStartEvent(String selector, Long sdxId, String userId,
            String backupId, String backupLocation) {
        super(selector, sdxId, userId, SdxOperationType.BACKUP);
        this.backupId = backupId;
        this.backupLocation = backupLocation;
    }

    public DatalakeDatabaseBackupStartEvent(String selector, SdxOperation drStatus, String userId,
                                            String backupId, String backupLocation) {
        super(selector, userId, drStatus);
        this.backupId = backupId;
        this.backupLocation = backupLocation;
    }

    public static DatalakeDatabaseBackupStartEvent from(DatalakeTriggerBackupEvent trigggerBackupEvent,
                                                        String backupId) {
        return  new DatalakeDatabaseBackupStartEvent(DATALAKE_DATABASE_BACKUP_EVENT.event(),
                trigggerBackupEvent.getDrStatus(),
                trigggerBackupEvent.getUserId(),
                backupId,
                trigggerBackupEvent.getBackupLocation());
    }

    public String getBackupId() {
        return backupId;
    }

    public String getBackupLocation() {
        return backupLocation;
    }
}
