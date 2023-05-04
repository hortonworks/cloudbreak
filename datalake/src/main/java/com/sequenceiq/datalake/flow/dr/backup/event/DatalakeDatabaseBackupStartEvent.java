package com.sequenceiq.datalake.flow.dr.backup.event;

import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupEvent.DATALAKE_DATABASE_BACKUP_EVENT;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.entity.operation.SdxOperation;
import com.sequenceiq.datalake.entity.operation.SdxOperationType;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.dr.event.DatalakeDatabaseDrStartBaseEvent;
import com.sequenceiq.sdx.api.model.SdxDatabaseBackupRequest;

public class DatalakeDatabaseBackupStartEvent extends DatalakeDatabaseDrStartBaseEvent {

    private final SdxDatabaseBackupRequest backupRequest;

    @JsonCreator
    public DatalakeDatabaseBackupStartEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("backupRequest") SdxDatabaseBackupRequest backupRequest) {

        super(selector, sdxId, userId, SdxOperationType.BACKUP, backupRequest.getSkipDatabaseNames());
        this.backupRequest = backupRequest;
    }

    @SuppressWarnings("checkstyle:ExecutableStatementCount")
    public DatalakeDatabaseBackupStartEvent(String selector, SdxOperation drStatus, String userId,
                                            String backupId, String backupLocation, List<String> skipDatabaseNames, int databaseMaxDurationInMin) {
        super(selector, drStatus.getSdxClusterId(), userId, drStatus, skipDatabaseNames);
        backupRequest = new SdxDatabaseBackupRequest();
        backupRequest.setBackupId(backupId);
        backupRequest.setBackupLocation(backupLocation);
        backupRequest.setCloseConnections(true);
        backupRequest.setSkipDatabaseNames(skipDatabaseNames);
        backupRequest.setDatabaseMaxDurationInMin(databaseMaxDurationInMin);
    }

    public static DatalakeDatabaseBackupStartEvent from(DatalakeTriggerBackupEvent triggerBackupEvent,
                                                        String backupId) {
        return new DatalakeDatabaseBackupStartEvent(DATALAKE_DATABASE_BACKUP_EVENT.event(),
                triggerBackupEvent.getDrStatus(),
                triggerBackupEvent.getUserId(),
                backupId,
                triggerBackupEvent.getBackupLocation(),
                triggerBackupEvent.getSkipDatabaseNames(),
                0);
    }

    public SdxDatabaseBackupRequest getBackupRequest() {
        return backupRequest;
    }

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(DatalakeDatabaseBackupStartEvent.class, other,
                event -> Objects.equals(backupRequest, event.backupRequest));
    }
}
