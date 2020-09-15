package com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.backup;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.BackupRestoreEvent;

public class DatalakeBackupFailedEvent extends BackupRestoreEvent {

    private Exception exception;

    private DetailedStackStatus detailedStatus;

    public DatalakeBackupFailedEvent(Long stackId, Exception exception, DetailedStackStatus detailedStatus) {
        super(stackId, null, null, null);
        this.exception = exception;
        this.detailedStatus = detailedStatus;
    }

    public static DatalakeBackupFailedEvent from(StackEvent event, Exception exception, DetailedStackStatus detailedStatus) {
        return new DatalakeBackupFailedEvent(event.getResourceId(), exception, detailedStatus);
    }

    public Exception getException() {
        return exception;
    }

    public DetailedStackStatus getDetailedStatus() {
        return detailedStatus;
    }
}
