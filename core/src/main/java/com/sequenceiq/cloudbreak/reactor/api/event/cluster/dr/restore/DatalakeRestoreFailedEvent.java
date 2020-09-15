package com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.restore;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.BackupRestoreEvent;

public class DatalakeRestoreFailedEvent extends BackupRestoreEvent {

    private Exception exception;

    private DetailedStackStatus detailedStatus;

    public DatalakeRestoreFailedEvent(Long stackId, Exception exception, DetailedStackStatus detailedStatus) {
        super(stackId, null, null, null);
        this.exception = exception;
        this.detailedStatus = detailedStatus;
    }

    public static DatalakeRestoreFailedEvent from(StackEvent event, Exception exception, DetailedStackStatus detailedStatus) {
        return new DatalakeRestoreFailedEvent(event.getResourceId(), exception, detailedStatus);
    }

    public Exception getException() {
        return exception;
    }

    public DetailedStackStatus getDetailedStatus() {
        return detailedStatus;
    }
}
