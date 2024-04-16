package com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.restore;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.BackupRestoreEvent;

public class DatabaseRestoreRequest extends BackupRestoreEvent {

    @JsonCreator
    public DatabaseRestoreRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("backupLocation") String backupLocation,
            @JsonProperty("backupId") String backupId,
            @JsonProperty("databaseMaxDurationInMin") int databaseMaxDurationInMin) {
        super(stackId, backupLocation, backupId, databaseMaxDurationInMin, false);
    }
}
