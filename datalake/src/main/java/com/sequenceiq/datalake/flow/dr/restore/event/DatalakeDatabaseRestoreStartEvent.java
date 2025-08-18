package com.sequenceiq.datalake.flow.dr.restore.event;

import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreEvent.DATALAKE_DATABASE_RESTORE_EVENT;

import java.util.Collections;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.entity.operation.SdxOperation;
import com.sequenceiq.datalake.entity.operation.SdxOperationType;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.dr.event.DatalakeDatabaseDrStartBaseEvent;

public class DatalakeDatabaseRestoreStartEvent extends DatalakeDatabaseDrStartBaseEvent {

    private static final int DEFAULT_DATABASE_MAX_DURATION_IN_MIN = 90;

    private final String backupId;

    private final String restoreId;

    private final String backupLocation;

    private final int databaseMaxDurationInMin;

    private final boolean validationOnly;

    public DatalakeDatabaseRestoreStartEvent(String selector, Long sdxId, String userId,
            String backupId, String restoreId, String backupLocation, int databaseMaxDurationInMin, boolean validationOnly) {
        super(selector, sdxId, userId, SdxOperationType.RESTORE, Collections.emptyList());
        this.backupId = backupId;
        this.restoreId = restoreId;
        this.backupLocation = backupLocation;
        this.databaseMaxDurationInMin = databaseMaxDurationInMin;
        this.validationOnly = validationOnly;
    }

    @JsonCreator
    public DatalakeDatabaseRestoreStartEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("drStatus") SdxOperation drStatus,
            @JsonProperty("userId") String userId,
            @JsonProperty("backupId") String backupId,
            @JsonProperty("restoreId") String restoreId,
            @JsonProperty("backupLocation") String backupLocation,
            @JsonProperty("databaseMaxDurationInMin") int databaseMaxDurationInMin,
            @JsonProperty("validationOnly") boolean validationOnly) {
        super(selector, sdxId, userId, drStatus, Collections.emptyList());
        this.backupId = backupId;
        this.restoreId = restoreId;
        this.backupLocation = backupLocation;
        this.databaseMaxDurationInMin = databaseMaxDurationInMin;
        this.validationOnly = validationOnly;
    }

    public static DatalakeDatabaseRestoreStartEvent from(DatalakeTriggerRestoreEvent triggerRestoreEvent, Long sdxId,
            String backupId, String restoreId) {
        return new DatalakeDatabaseRestoreStartEvent(DATALAKE_DATABASE_RESTORE_EVENT.event(),
                sdxId,
                triggerRestoreEvent.getDrStatus(),
                triggerRestoreEvent.getUserId(),
                backupId,
                restoreId,
                triggerRestoreEvent.getBackupLocation(),
                DEFAULT_DATABASE_MAX_DURATION_IN_MIN,
                triggerRestoreEvent.isValidationOnly());
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

    public int getDatabaseMaxDurationInMin() {
        return databaseMaxDurationInMin;
    }

    public boolean isValidationOnly() {
        return validationOnly;
    }

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(DatalakeDatabaseRestoreStartEvent.class, other,
                event -> Objects.equals(backupId, event.backupId)
                        && Objects.equals(restoreId, event.restoreId)
                        && Objects.equals(backupLocation, event.backupLocation));
    }
}
