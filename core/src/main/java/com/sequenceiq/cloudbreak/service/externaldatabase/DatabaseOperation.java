package com.sequenceiq.cloudbreak.service.externaldatabase;

import java.util.function.Function;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.redbeams.api.model.common.Status;

public enum DatabaseOperation {

    CREATION(Status::isAvailable, status -> status.isDeleteInProgressOrCompleted() || Status.CREATE_FAILED.equals(status),
            ResourceEvent.CLUSTER_EXTERNAL_DATABASE_CREATION_FINISHED, ResourceEvent.CLUSTER_EXTERNAL_DATABASE_CREATION_FAILED),
    DELETION(Status.DELETE_COMPLETED::equals, Status.DELETE_FAILED::equals,
            ResourceEvent.CLUSTER_EXTERNAL_DATABASE_DELETION_FINISHED, ResourceEvent.CLUSTER_EXTERNAL_DATABASE_DELETION_FAILED),
    START(Status::isAvailable, Status.START_FAILED::equals,
            ResourceEvent.CLUSTER_EXTERNAL_DATABASE_START_FINISHED, ResourceEvent.CLUSTER_EXTERNAL_DATABASE_START_FAILED),
    STOP(Status.STOPPED::equals, Status.STOP_FAILED::equals,
            ResourceEvent.CLUSTER_EXTERNAL_DATABASE_STOP_FINISHED, ResourceEvent.CLUSTER_EXTERNAL_DATABASE_STOP_FAILED),

    UPGRADE(Status::isAvailable, Status.UPGRADE_FAILED::equals,
            ResourceEvent.CLUSTER_RDS_UPGRADE_FINISHED, ResourceEvent.CLUSTER_RDS_UPGRADE_FAILED),

    SSL_ROTATED(Status::isAvailable, status -> status.isDeleteInProgressOrCompleted() || Status.SSL_ROTATE_FAILED.equals(status),
            ResourceEvent.ROTATE_RDS_CERTIFICATE_FINISHED, ResourceEvent.ROTATE_RDS_CERTIFICATE_FAILED),;

    private final Function<Status, Boolean> exitCriteria;

    private final Function<Status, Boolean> failureCriteria;

    private final ResourceEvent finishedEvent;

    private final ResourceEvent failedEvent;

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
