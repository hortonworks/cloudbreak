package com.sequenceiq.datalake.flow.dr.restore.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.datalakedr.DatalakeDrSkipOptions;
import com.sequenceiq.datalake.entity.operation.SdxOperationType;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.dr.event.DatalakeDatabaseDrStartBaseEvent;
import com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreFailureReason;

public class DatalakeTriggerRestoreEvent extends DatalakeDatabaseDrStartBaseEvent {

    private final String backupId;

    private final DatalakeRestoreFailureReason reason;

    private final String backupLocation;

    private final String backupLocationOverride;

    private final DatalakeDrSkipOptions skipOptions;

    private final int fullDrMaxDurationInMin;

    @SuppressWarnings("checkstyle:ExecutableStatementCount")
    @JsonCreator
    public DatalakeTriggerRestoreEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("sdxName") String sdxName,
            @JsonProperty("userId") String userId,
            @JsonProperty("backupId") String backupId,
            @JsonProperty("backupLocation") String backupLocation,
            @JsonProperty("backupLocationOverride") String backupLocationOverride,
            @JsonProperty("skipOptions") DatalakeDrSkipOptions skipOptions,
            @JsonProperty("reason") DatalakeRestoreFailureReason reason,
            @JsonProperty("fullDrMaxDurationInMin") int fullDrMaxDurationInMin) {
        super(selector, sdxId, sdxName, userId, SdxOperationType.RESTORE);
        this.backupId = backupId;
        this.backupLocation = backupLocation;
        this.backupLocationOverride = backupLocationOverride;
        this.skipOptions = skipOptions;
        this.reason = reason;
        this.fullDrMaxDurationInMin = fullDrMaxDurationInMin;
    }

    public String getBackupId() {
        return backupId;
    }

    public String getBackupLocation() {
        return backupLocation;
    }

    public DatalakeDrSkipOptions getSkipOptions() {
        return skipOptions;
    }

    public String getBackupLocationOverride() {
        return backupLocationOverride;
    }

    public DatalakeRestoreFailureReason getReason() {
        return reason;
    }

    public int getFullDrMaxDurationInMin() {
        return fullDrMaxDurationInMin;
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
