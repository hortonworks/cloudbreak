package com.sequenceiq.datalake.entity;

public enum DatalakeStatusEnum {

    REQUESTED,
    WAIT_FOR_ENVIRONMENT,
    ENVIRONMENT_CREATED,
    STACK_CREATION_IN_PROGRESS,
    STACK_DELETED,
    STACK_DELETION_IN_PROGRESS,
    EXTERNAL_DATABASE_CREATION_IN_PROGRESS,
    EXTERNAL_DATABASE_DELETION_IN_PROGRESS,
    RUNNING,
    PROVISIONING_FAILED,
    REPAIR_IN_PROGRESS,
    REPAIR_FAILED,
    DELETE_REQUESTED,
    DELETED,
    DELETE_FAILED,
    UNHEALTHY;

    public boolean isDeleteInProgressOrCompleted() {
        return EXTERNAL_DATABASE_DELETION_IN_PROGRESS.equals(this)
                || STACK_DELETED.equals(this)
                || STACK_DELETION_IN_PROGRESS.equals(this)
                || DELETE_REQUESTED.equals(this)
                || DELETED.equals(this);
    }

}
