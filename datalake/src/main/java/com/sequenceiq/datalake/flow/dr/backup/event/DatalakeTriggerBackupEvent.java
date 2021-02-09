package com.sequenceiq.datalake.flow.dr.backup.event;

import com.sequenceiq.datalake.entity.operation.SdxOperationType;
import com.sequenceiq.datalake.flow.dr.event.DatalakeDatabaseDrStartBaseEvent;

public class DatalakeTriggerBackupEvent extends DatalakeDatabaseDrStartBaseEvent {

    private final String backupLocation;

    private final String backupName;

    public DatalakeTriggerBackupEvent(String selector, Long sdxId, String userId, String backupLocation, String backupName) {
        super(selector, sdxId, userId, SdxOperationType.BACKUP);
        this.backupLocation = backupLocation;
        this.backupName = backupName;
    }

    public String getBackupLocation() {
        return backupLocation;
    }

    public String getBackupName() {
        return backupName;
    }
}
