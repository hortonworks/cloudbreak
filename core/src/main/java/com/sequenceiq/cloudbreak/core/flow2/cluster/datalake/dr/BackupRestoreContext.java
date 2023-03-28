package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr;

import java.util.List;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class BackupRestoreContext extends CommonContext {

    private final Long stackId;

    private final String backupLocation;

    private final String backupId;

    private final boolean closeConnections;

    private final List<String> skipDatabaseNames;

    private final int databaseMaxDurationInMin;

    public BackupRestoreContext(FlowParameters flowParameters, StackEvent event, String backupLocation, String backupId,
                                boolean closeConnections, List<String> skipDatabaseNames, int databaseMaxDurationInMin) {
        super(flowParameters);
        this.stackId = event.getResourceId();
        this.backupLocation = backupLocation;
        this.backupId = backupId;
        this.closeConnections = closeConnections;
        this.skipDatabaseNames = skipDatabaseNames;
        this.databaseMaxDurationInMin = databaseMaxDurationInMin;
    }

    public static BackupRestoreContext from(FlowParameters flowParameters, StackEvent event, String backupLocation, String backupId,
                                            boolean closeConnections, List<String> skipDatabaseNames, int databaseMaxDurationInMin) {
        return new BackupRestoreContext(flowParameters, event, backupLocation, backupId, closeConnections, skipDatabaseNames, databaseMaxDurationInMin);
    }

    public Long getStackId() {
        return stackId;
    }

    public String getBackupLocation() {
        return backupLocation;
    }

    public String getBackupId() {
        return backupId;
    }

    public boolean getCloseConnections() {
        return closeConnections;
    }

    public List<String> getSkipDatabaseNames() {
        return skipDatabaseNames;
    }

    public int getDatabaseMaxDurationInMin() {
        return databaseMaxDurationInMin;
    }
}
