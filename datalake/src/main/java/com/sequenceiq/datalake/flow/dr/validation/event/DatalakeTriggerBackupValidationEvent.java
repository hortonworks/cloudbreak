package com.sequenceiq.datalake.flow.dr.validation.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.datalake.entity.operation.SdxOperationType;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.dr.event.DatalakeDatabaseDrStartBaseEvent;
import com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupFailureReason;

import reactor.rx.Promise;

public class DatalakeTriggerBackupValidationEvent extends DatalakeDatabaseDrStartBaseEvent {

    private final String backupLocation;

    private final DatalakeBackupFailureReason reason;

    @JsonCreator
    public DatalakeTriggerBackupValidationEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("backupLocation") String backupLocation,
            @JsonProperty("reason") DatalakeBackupFailureReason reason,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(selector, sdxId, userId, SdxOperationType.BACKUP, accepted);
        this.backupLocation = backupLocation;
        this.reason = reason;
    }

    public DatalakeTriggerBackupValidationEvent(String selector, Long sdxId, String userId, String backupLocation,
            DatalakeBackupFailureReason reason) {
        super(selector, sdxId, userId, SdxOperationType.BACKUP);
        this.backupLocation = backupLocation;
        this.reason = reason;
    }

    public String getBackupLocation() {
        return backupLocation;
    }

    public DatalakeBackupFailureReason getReason() {
        return reason;
    }

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(DatalakeTriggerBackupValidationEvent.class, other,
                event -> Objects.equals(backupLocation, event.backupLocation)
                        && Objects.equals(reason, event.reason));
    }
}
