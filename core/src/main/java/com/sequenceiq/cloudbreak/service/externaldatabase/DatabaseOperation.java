package com.sequenceiq.cloudbreak.service.externaldatabase;

import java.util.function.Function;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.redbeams.api.model.common.Status;

public enum DatabaseOperation {

    CREATION(Status::isAvailable, status -> status.isDeleteInProgressOrCompleted() || Status.CREATE_FAILED.equals(status),
            ResourceEvent.CLUSTER_EXTERNAL_DATABASE_CREATION_FINISHED, ResourceEvent.CLUSTER_EXTERNAL_DATABASE_CREATION_FAILED),
    DELETION(Status.DELETE_COMPLETED::equals, Status.DELETE_FAILED::equals,
            ResourceEvent.CLUSTER_EXTERNAL_DATABASE_DELETION_FINISHED, ResourceEvent.CLUSTER_EXTERNAL_DATABASE_DELETION_FAILED);

    private Function<Status, Boolean> exitCriteria;

    private Function<Status, Boolean> failureCriteria;

    private ResourceEvent finishedEvent;

    private ResourceEvent failedEvent;

    DatabaseOperation(Function<Status, Boolean> exitCriteria, Function<Status, Boolean> failureCriteria,
            ResourceEvent finishedEvent, ResourceEvent failedEvent) {
        this.exitCriteria = exitCriteria;
        this.failureCriteria = failureCriteria;
        this.finishedEvent = finishedEvent;
        this.failedEvent = failedEvent;
    }

    public ResourceEvent getFinishedEvent() {
        return finishedEvent;
    }

    public ResourceEvent getFailedEvent() {
        return failedEvent;
    }

    public Function<Status, Boolean> getExitCriteria() {
        return exitCriteria;
    }

    public Function<Status, Boolean> getFailureCriteria() {
        return failureCriteria;
    }
}
