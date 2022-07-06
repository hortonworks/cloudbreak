package com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.restore;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.BackupRestoreEvent;

public class DatabaseRestoreFailedEvent extends BackupRestoreEvent {

    private final Exception exception;

    private final DetailedStackStatus detailedStatus;

    @JsonCreator
    public DatabaseRestoreFailedEvent(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("detailedStatus") DetailedStackStatus detailedStatus) {
        super(stackId, null, null);
        this.exception = exception;
        this.detailedStatus = detailedStatus;
    }

    public static DatabaseRestoreFailedEvent from(StackEvent event, Exception exception, DetailedStackStatus detailedStatus) {
        return new DatabaseRestoreFailedEvent(event.getResourceId(), exception, detailedStatus);
    }

    public Exception getException() {
        return exception;
    }

    public DetailedStackStatus getDetailedStatus() {
        return detailedStatus;
    }
}
