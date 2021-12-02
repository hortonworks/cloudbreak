package com.sequenceiq.datalake.flow.dr.restore.event;

import java.util.Objects;

import com.sequenceiq.datalake.entity.operation.SdxOperationType;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.dr.event.DatalakeDatabaseDrStartBaseEvent;
import com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreFailureReason;

public class DatalakeTriggerRestoreEvent extends DatalakeDatabaseDrStartBaseEvent {

    private final String backupId;

    private final DatalakeRestoreFailureReason reason;

    private final String backupLocation;

    private final String backupLocationOverride;

    public DatalakeTriggerRestoreEvent(String selector, Long sdxId, String userId,
            String backupId, String backupLocation, String backupLocationOverride, DatalakeRestoreFailureReason reason) {
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

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(DatalakeTriggerRestoreEvent.class, other,
                event -> Objects.equals(backupId, event.backupId)
                        && Objects.equals(reason, event.reason)
                        && Objects.equals(backupLocation, event.backupLocation)
                        && Objects.equals(backupLocationOverride, event.backupLocationOverride));
    }

}
