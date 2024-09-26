package com.sequenceiq.datalake.flow.dr.validation.event;

import java.util.Collections;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.datalake.entity.operation.SdxOperationType;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.dr.event.DatalakeDatabaseDrStartBaseEvent;
import com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreFailureReason;

public class DatalakeTriggerRestoreValidationEvent extends DatalakeDatabaseDrStartBaseEvent {

    private final String backupLocation;

    private final DatalakeRestoreFailureReason reason;

    @JsonCreator
    public DatalakeTriggerRestoreValidationEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("backupLocation") String backupLocation,
            @JsonProperty("reason") DatalakeRestoreFailureReason reason,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(selector, sdxId, userId, SdxOperationType.RESTORE, Collections.emptyList(), accepted);
        this.backupLocation = backupLocation;
        this.reason = reason;
    }

    public DatalakeTriggerRestoreValidationEvent(String selector, Long sdxId, String userId, String backupLocation,
            DatalakeRestoreFailureReason reason) {
        super(selector, sdxId, userId, SdxOperationType.RESTORE, Collections.emptyList());
        this.backupLocation = backupLocation;
        this.reason = reason;
    }

    public String getBackupLocation() {
        return backupLocation;
    }

    public DatalakeRestoreFailureReason getReason() {
        return reason;
    }

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(DatalakeTriggerRestoreValidationEvent.class, other,
                event -> Objects.equals(backupLocation, event.backupLocation)
                        && Objects.equals(reason, event.reason));
    }
}
