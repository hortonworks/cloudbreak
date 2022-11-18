package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.BackupRestoreEvent;

public class DatabaseBackupTriggerEvent extends BackupRestoreEvent {

    public DatabaseBackupTriggerEvent(String selector, Long stackId, String backupLocation, String backupId,
        boolean closeConnections, List<String> skipDatabaseNames) {
        super(selector, stackId, backupLocation, backupId, closeConnections, skipDatabaseNames);
    }

    @JsonCreator
    public DatabaseBackupTriggerEvent(
            @JsonProperty("selector") String event,
            @JsonProperty("resourceId") Long resourceId,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted,
            @JsonProperty("backupLocation") String backupLocation,
            @JsonProperty("backupId") String backupId,
            @JsonProperty("closeConnections") boolean closeConnections,
            @JsonProperty("skipDatabaseNames") List<String> skipDatabaseNames) {
        super(event, resourceId, accepted, backupLocation, backupId, closeConnections, skipDatabaseNames);
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(DatabaseBackupTriggerEvent.class, other,
                event -> Objects.equals(getBackupId(), event.getBackupId())
                        && Objects.equals(getBackupLocation(), event.getBackupLocation())
                        && isCloseConnections() == event.isCloseConnections());
    }
}
