package com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.backup;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.BackupRestoreEvent;

public class DatabaseBackupSuccess extends BackupRestoreEvent {

    @JsonCreator
    public DatabaseBackupSuccess(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("dryRun") boolean dryRun) {
        super(stackId, null, null, dryRun);
    }
}
