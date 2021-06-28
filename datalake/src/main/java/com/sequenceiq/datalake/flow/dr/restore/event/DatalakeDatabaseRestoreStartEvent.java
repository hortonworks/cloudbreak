package com.sequenceiq.datalake.flow.dr.restore.event;

import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreEvent.DATALAKE_DATABASE_RESTORE_EVENT;

import com.sequenceiq.datalake.entity.operation.SdxOperation;
import com.sequenceiq.datalake.entity.operation.SdxOperationType;
import com.sequenceiq.datalake.flow.dr.event.DatalakeDatabaseDrStartBaseEvent;

public class DatalakeDatabaseRestoreStartEvent extends DatalakeDatabaseDrStartBaseEvent {
    private final String backupId;

    private final String restoreId;

    private final String backupLocation;

    public DatalakeDatabaseRestoreStartEvent(String selector, Long sdxId, String userId,
            String backupId, String restoreId, String backupLocation) {
        super(selector, sdxId, userId, SdxOperationType.RESTORE);
        this.backupId = backupId;
        this.restoreId = restoreId;
        this.backupLocation = backupLocation;
    }

    public DatalakeDatabaseRestoreStartEvent(String selector, SdxOperation drStatus, String userId,
            String backupId, String restoreId, String backupLocation) {
        super(selector, userId, drStatus);
        this.backupId = backupId;
        this.restoreId = restoreId;
        this.backupLocation = backupLocation;
    }

    public static DatalakeDatabaseRestoreStartEvent from(DatalakeTriggerRestoreEvent trigggerRestoreEvent,
            String restoreId) {
        return new DatalakeDatabaseRestoreStartEvent(DATALAKE_DATABASE_RESTORE_EVENT.event(),
                trigggerRestoreEvent.getDrStatus(),
                trigggerRestoreEvent.getUserId(),
                trigggerRestoreEvent.getBackupId(),
                restoreId,
                trigggerRestoreEvent.getBackupLocation());
    }

    public String getBackupId() {
        return backupId;
    }

    public String getRestoreId() {
        return restoreId;
    }

    public String getBackupLocation() {
        return backupLocation;
    }

}
