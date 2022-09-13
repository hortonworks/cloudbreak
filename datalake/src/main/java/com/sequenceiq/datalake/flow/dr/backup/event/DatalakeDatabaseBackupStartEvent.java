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

    public DatalakeDatabaseBackupStartEvent(String selector, SdxOperation drStatus, String userId,
                                            String backupId, String backupLocation, List<String> skipDatabaseNames) {
        super(selector, drStatus.getSdxClusterId(), userId, drStatus, skipDatabaseNames);
        backupRequest = new SdxDatabaseBackupRequest();
        backupRequest.setBackupId(backupId);
        backupRequest.setBackupLocation(backupLocation);
        backupRequest.setCloseConnections(true);
        backupRequest.setSkipDatabaseNames(skipDatabaseNames);
    }

    public static DatalakeDatabaseBackupStartEvent from(DatalakeTriggerBackupEvent trigggerBackupEvent,
                                                        String backupId) {
        return  new DatalakeDatabaseBackupStartEvent(DATALAKE_DATABASE_BACKUP_EVENT.event(),
                trigggerBackupEvent.getDrStatus(),
                trigggerBackupEvent.getUserId(),
                backupId,
                trigggerBackupEvent.getBackupLocation(),
                trigggerBackupEvent.getSkipDatabaseNames());
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
