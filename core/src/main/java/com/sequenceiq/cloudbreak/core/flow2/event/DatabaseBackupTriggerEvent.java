package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.BackupRestoreEvent;

import reactor.rx.Promise;

public class DatabaseBackupTriggerEvent extends BackupRestoreEvent {

    public DatabaseBackupTriggerEvent(String selector, Long stackId, String backupLocation, String backupId, boolean closeConnections) {
        super(selector, stackId, backupLocation, backupId, closeConnections);
    }

    public DatabaseBackupTriggerEvent(String selector, Long stackId, String backupLocation, String backupId) {
        super(selector, stackId, backupLocation, backupId);
    }

    @JsonCreator
    public DatabaseBackupTriggerEvent(
            @JsonProperty("selector") String event,
            @JsonProperty("resourceId") Long resourceId,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted,
            @JsonProperty("backupLocation") String backupLocation,
            @JsonProperty("backupId") String backupId,
            @JsonProperty("closeConnections") boolean closeConnections) {
        super(event, resourceId, accepted, backupLocation, backupId, closeConnections);
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(DatabaseBackupTriggerEvent.class, other,
                event -> Objects.equals(getBackupId(), event.getBackupId())
                        && Objects.equals(getBackupLocation(), event.getBackupLocation())
                        && isCloseConnections() == event.isCloseConnections());
    }
}
