package com.sequenceiq.datalake.flow.dr.backup.event;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.datalake.entity.operation.SdxOperationType;
import com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupFailureReason;
import com.sequenceiq.datalake.flow.dr.event.DatalakeDatabaseDrStartBaseEvent;
import reactor.rx.Promise;

public class DatalakeTriggerBackupEvent extends DatalakeDatabaseDrStartBaseEvent {

    private final String backupLocation;

    private final String backupName;

    private final DatalakeBackupFailureReason reason;

    public DatalakeTriggerBackupEvent(String selector, Long sdxId, String userId, String backupLocation,
            String backupName, DatalakeBackupFailureReason reason, Promise<AcceptResult> accepted) {
        super(selector, sdxId, userId, SdxOperationType.BACKUP, accepted);
        this.backupLocation = backupLocation;
        this.backupName = backupName;
        this.reason = reason;
    }

    public DatalakeTriggerBackupEvent(String selector, Long sdxId, String userId, String backupLocation,
            String backupName, DatalakeBackupFailureReason reason) {
        super(selector, sdxId, userId, SdxOperationType.BACKUP);
        this.backupLocation = backupLocation;
        this.backupName = backupName;
        this.reason = reason;
    }

    public String getBackupLocation() {
        return backupLocation;
    }

    public String getBackupName() {
        return backupName;
    }

    public DatalakeBackupFailureReason getReason() {
        return reason;
    }
}
