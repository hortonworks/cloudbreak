package com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.backup;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.BackupRestoreEvent;

public class DatabaseBackupRequest extends BackupRestoreEvent {

    @JsonCreator
    public DatabaseBackupRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("backupLocation") String backupLocation,
            @JsonProperty("backupId") String backupId,
            @JsonProperty("closeConnections") boolean closeConnections,
            @JsonProperty("skipDatabaseNames") List<String> skipDatabaseNames,
            @JsonProperty("databaseMaxDurationInMin") int databaseMaxDurationInMin,
            @JsonProperty("dryRun") boolean dryRun) {
        super(null, stackId, backupLocation, backupId, closeConnections, skipDatabaseNames, databaseMaxDurationInMin, dryRun);
    }
}
