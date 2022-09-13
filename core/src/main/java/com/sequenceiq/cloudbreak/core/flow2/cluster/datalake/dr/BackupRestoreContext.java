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

    public BackupRestoreContext(FlowParameters flowParameters, StackEvent event, String backupLocation, String backupId,
            boolean closeConnections, List<String> skipDatabaseNames) {
        super(flowParameters);
        this.stackId = event.getResourceId();
        this.backupLocation = backupLocation;
        this.backupId = backupId;
        this.closeConnections = closeConnections;
        this.skipDatabaseNames = skipDatabaseNames;
    }

    public static BackupRestoreContext from(FlowParameters flowParameters, StackEvent event, String backupLocation, String backupId,
            boolean closeConnections, List<String> skipDatabaseNames) {
        return new BackupRestoreContext(flowParameters, event, backupLocation, backupId, closeConnections, skipDatabaseNames);
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
}
