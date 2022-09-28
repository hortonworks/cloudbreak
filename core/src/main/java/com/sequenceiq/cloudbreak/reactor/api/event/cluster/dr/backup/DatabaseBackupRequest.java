package com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.backup;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.BackupRestoreEvent;

public class DatabaseBackupRequest extends BackupRestoreEvent {

    @JsonCreator
    public DatabaseBackupRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("backupLocation") String backupLocation,
            @JsonProperty("backupId") String backupId,
            @JsonProperty("closeConnections") boolean closeConnections) {
        super(stackId, backupLocation, backupId, closeConnections);
    }
}
