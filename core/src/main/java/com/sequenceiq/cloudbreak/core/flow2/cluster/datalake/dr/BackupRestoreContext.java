package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class BackupRestoreContext extends CommonContext {

    private final Long stackId;

    private final String backupLocation;

    private final String backupId;

    private final boolean closeConnections;

    public BackupRestoreContext(FlowParameters flowParameters, StackEvent event, String backupLocation, String backupId, boolean closeConnections) {
        super(flowParameters);
        this.stackId = event.getResourceId();
        this.backupLocation = backupLocation;
        this.backupId = backupId;
        this.closeConnections = closeConnections;
    }

    public BackupRestoreContext(FlowParameters flowParameters, Long stackId, String backupLocation, String backupId) {
        super(flowParameters);
        this.stackId = stackId;
        this.backupLocation = backupLocation;
        this.backupId = backupId;
        this.closeConnections = true;
    }

    public static BackupRestoreContext from(FlowParameters flowParameters, StackEvent event, String backupLocation, String backupId, boolean closeConnections) {
        return new BackupRestoreContext(flowParameters, event, backupLocation, backupId, closeConnections);
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
}
