package com.sequenceiq.datalake.flow.dr.backup.event;

import com.sequenceiq.datalake.entity.operation.SdxOperationType;
import com.sequenceiq.datalake.flow.dr.event.DatalakeDatabaseDrStartBaseEvent;

public class DatalakeDatabaseBackupStartEvent extends DatalakeDatabaseDrStartBaseEvent {

    private final String backupId;

    private final String backupLocation;

    public DatalakeDatabaseBackupStartEvent(String selector, Long sdxId, String userId,
            String backupId, String backupLocation) {
        super(selector, sdxId, userId, SdxOperationType.BACKUP);
        this.backupId = backupId;
        this.backupLocation = backupLocation;
    }

    public String getBackupId() {
        return backupId;
    }

    public String getBackupLocation() {
        return backupLocation;
    }
}
