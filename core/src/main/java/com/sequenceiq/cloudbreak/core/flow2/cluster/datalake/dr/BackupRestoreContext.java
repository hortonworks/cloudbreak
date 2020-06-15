package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class BackupRestoreContext extends CommonContext {

    private final Long stackId;

    private final String backupLocation;

    private final String backupId;

    public BackupRestoreContext(FlowParameters flowParameters, StackEvent event, String backupLocation, String backupId) {
        super(flowParameters);
        this.stackId = event.getResourceId();
        this.backupLocation = backupLocation;
        this.backupId = backupId;
    }

    public BackupRestoreContext(FlowParameters flowParameters, Long stackId, String backupLocation, String backupId) {
        super(flowParameters);
        this.stackId = stackId;
        this.backupLocation = backupLocation;
        this.backupId = backupId;
    }

    public static BackupRestoreContext from(FlowParameters flowParameters, StackEvent event, String backupLocation, String backupId) {
        return new BackupRestoreContext(flowParameters, event, backupLocation, backupId);
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
}
