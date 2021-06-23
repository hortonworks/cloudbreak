package com.sequenceiq.datalake.flow.dr.restore.event;

import com.sequenceiq.datalake.entity.operation.SdxOperationType;
import com.sequenceiq.datalake.flow.dr.event.DatalakeDatabaseDrStartBaseEvent;
import com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreReason;

public class DatalakeTriggerRestoreEvent extends DatalakeDatabaseDrStartBaseEvent {

    private final String backupId;

    private final DatalakeRestoreReason reason;

    private final String backupLocation;

    private final String backupLocationOverride;

    public DatalakeTriggerRestoreEvent(String selector, Long sdxId, String userId,
            String backupId, String backupLocation, String backupLocationOverride, DatalakeRestoreReason reason) {
        super(selector, sdxId, userId, SdxOperationType.RESTORE);
        this.backupId = backupId;
        this.backupLocation = backupLocation;
        this.backupLocationOverride = backupLocationOverride;
        this.reason = reason;
    }

    public String getBackupId() {
        return backupId;
    }

    public String getBackupLocation() {
        return backupLocation;
    }

    public String getBackupLocationOverride() {
        return backupLocationOverride;
    }

}
