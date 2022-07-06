package com.sequenceiq.datalake.flow.dr.backup.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.datalake.entity.operation.SdxOperationType;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupFailureReason;
import com.sequenceiq.datalake.flow.dr.event.DatalakeDatabaseDrStartBaseEvent;

import reactor.rx.Promise;

public class DatalakeTriggerBackupEvent extends DatalakeDatabaseDrStartBaseEvent {

    private final String backupLocation;

    private final String backupName;

    private final DatalakeBackupFailureReason reason;

    @JsonCreator
    public DatalakeTriggerBackupEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("backupLocation") String backupLocation,
            @JsonProperty("backupName") String backupName,
            @JsonProperty("reason") DatalakeBackupFailureReason reason,
            @JsonIgnoreDeserialization Promise<AcceptResult> accepted) {
        super(selector, sdxId, userId, SdxOperationType.BACKUP, accepted);
        this.backupLocation = backupLocation;
        this.backupName = backupName;
        this.reason = reason;
    }

    public DatalakeTriggerBackupEvent(String selector, Long sdxId, String userId, String backupLocation, String backupName,
            DatalakeBackupFailureReason reason) {
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

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(DatalakeTriggerBackupEvent.class, other,
                event -> Objects.equals(backupLocation, event.backupLocation)
                        && Objects.equals(backupName, event.backupName)
                        && Objects.equals(reason, event.reason));
    }
}
