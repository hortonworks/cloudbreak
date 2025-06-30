package com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class BackupRestoreEvent extends StackEvent {

    private final String backupLocation;

    private final String backupId;

    private final boolean closeConnections;

    private final List<String> skipDatabaseNames;

    private final int databaseMaxDurationInMin;

    private final boolean dryRun;

    public BackupRestoreEvent(Long stackId, String backupLocation, String backupId) {
        this(null, stackId, backupLocation, backupId, false);
    }

    public BackupRestoreEvent(Long stackId, String backupLocation, String backupId, boolean dryRun) {
        this(null, stackId, backupLocation, backupId, dryRun);
    }

    public BackupRestoreEvent(Long stackId, String backupLocation, String backupId, int databaseMaxDurationInMin, boolean dryRun) {
        this(null, stackId, backupLocation, backupId, databaseMaxDurationInMin, dryRun);
    }

    public BackupRestoreEvent(String selector, Long stackId, String backupLocation, String backupId, boolean dryRun) {
        super(selector, stackId);
        this.backupLocation = backupLocation;
        this.backupId = backupId;
        this.dryRun = dryRun;
        this.closeConnections = true;
        this.skipDatabaseNames = Collections.emptyList();
        this.databaseMaxDurationInMin = 0;
    }

    public BackupRestoreEvent(String selector, Long stackId, String backupLocation, String backupId, int databaseMaxDurationInMin, boolean dryRun) {
        super(selector, stackId);
        this.backupLocation = backupLocation;
        this.backupId = backupId;
        this.dryRun = dryRun;
        this.closeConnections = true;
        this.skipDatabaseNames = Collections.emptyList();
        this.databaseMaxDurationInMin = databaseMaxDurationInMin;
    }

    public BackupRestoreEvent(String selector, Long stackId, String backupLocation, String backupId, boolean closeConnections, List<String> skipDatabaseNames,
            int databaseMaxDurationInMin, boolean dryRun) {
        super(selector, stackId);
        this.backupLocation = backupLocation;
        this.backupId = backupId;
        this.closeConnections = closeConnections;
        this.skipDatabaseNames = skipDatabaseNames;
        this.databaseMaxDurationInMin = databaseMaxDurationInMin;
        this.dryRun = dryRun;
    }

    public BackupRestoreEvent(String selector, Long stackId, Promise<AcceptResult> accepted, String backupLocation, String backupId, boolean closeConnections,
            int databaseMaxDurationInMin, boolean dryRun) {
        super(selector, stackId, accepted);
        this.backupLocation = backupLocation;
        this.backupId = backupId;
        this.closeConnections = closeConnections;
        this.dryRun = dryRun;
        this.skipDatabaseNames = Collections.emptyList();
        this.databaseMaxDurationInMin = databaseMaxDurationInMin;
    }

    @JsonCreator
    public BackupRestoreEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted,
            @JsonProperty("backupLocation") String backupLocation,
            @JsonProperty("backupId") String backupId,
            @JsonProperty("closeConnections") boolean closeConnections,
            @JsonProperty("skipDatabaseNames") List<String> skipDatabaseNames,
            @JsonProperty("databaseMaxDurationInMin") int databaseMaxDurationInMin,
            @JsonProperty("dryRun") boolean dryRun) {
        super(selector, stackId, accepted);
        this.backupLocation = backupLocation;
        this.backupId = backupId;
        this.closeConnections = closeConnections;
        this.skipDatabaseNames = skipDatabaseNames;
        this.databaseMaxDurationInMin = databaseMaxDurationInMin;
        this.dryRun = dryRun;
    }

    public String getBackupLocation() {
        return backupLocation;
    }

    public String getBackupId() {
        return backupId;
    }

    public boolean isCloseConnections() {
        return closeConnections;
    }

    public List<String> getSkipDatabaseNames() {
        return skipDatabaseNames;
    }

    public int getDatabaseMaxDurationInMin() {
        return databaseMaxDurationInMin;
    }

    public boolean isDryRun() {
        return dryRun;
    }
}
