package com.sequenceiq.datalake.service.sdx;

import java.util.function.Function;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.redbeams.api.model.common.Status;

public enum SdxDatabaseOperation {

    CREATION(Status::isAvailable, status -> status.isDeleteInProgressOrCompleted() || Status.CREATE_FAILED.equals(status),
            ResourceEvent.SDX_RDS_CREATION_FINISHED, ResourceEvent.SDX_RDS_CREATION_FAILED),
    DELETION(Status.DELETE_COMPLETED::equals, Status.DELETE_FAILED::equals,
            ResourceEvent.SDX_RDS_DELETION_FINISHED, ResourceEvent.SDX_RDS_DELETION_FAILED),
    START(Status::isAvailable, Status.START_FAILED::equals,
            ResourceEvent.SDX_RDS_START_FINISHED, ResourceEvent.SDX_RDS_START_FAILED),
    STOP(Status.STOPPED::equals, Status.STOP_FAILED::equals,
            ResourceEvent.SDX_RDS_STOP_FINISHED, ResourceEvent.SDX_RDS_STOP_FAILED);

    private Function<Status, Boolean> exitCriteria;

    private Function<Status, Boolean> failureCriteria;

    private ResourceEvent finishedEvent;

    private ResourceEvent failedEvent;

    SdxDatabaseOperation(Function<Status, Boolean> exitCriteria, Function<Status, Boolean> failureCriteria,
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
