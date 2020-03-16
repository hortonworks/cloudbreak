package com.sequenceiq.datalake.entity;

import com.sequenceiq.cloudbreak.event.ResourceEvent;

public enum DatalakeStatusEnum {

    REQUESTED(ResourceEvent.SDX_CLUSTER_PROVISION_STARTED),
    WAIT_FOR_ENVIRONMENT(ResourceEvent.SDX_WAITING_FOR_ENVIRONMENT),
    ENVIRONMENT_CREATED(ResourceEvent.SDX_ENVIRONMENT_FINISHED),
    STACK_CREATION_IN_PROGRESS(ResourceEvent.SDX_CLUSTER_PROVISION_STARTED),
    STACK_CREATION_FINISHED(ResourceEvent.SDX_CLUSTER_PROVISION_FINISHED),
    STACK_DELETED(ResourceEvent.SDX_CLUSTER_DELETED),
    STACK_DELETION_IN_PROGRESS(ResourceEvent.SDX_CLUSTER_DELETION_STARTED),
    EXTERNAL_DATABASE_CREATION_IN_PROGRESS(ResourceEvent.SDX_RDS_CREATION_STARTED),
    EXTERNAL_DATABASE_CREATED(ResourceEvent.SDX_RDS_CREATION_FINISHED),
    EXTERNAL_DATABASE_DELETION_IN_PROGRESS(ResourceEvent.SDX_RDS_DELETION_STARTED),
    RUNNING(ResourceEvent.SDX_CLUSTER_CREATED),
    PROVISIONING_FAILED(ResourceEvent.SDX_CLUSTER_CREATION_FAILED),
    REPAIR_IN_PROGRESS(ResourceEvent.SDX_REPAIR_STARTED),
    REPAIR_FAILED(ResourceEvent.SDX_REPAIR_FAILED),
    CHANGE_IMAGE_IN_PROGRESS(ResourceEvent.SDX_CHANGE_IMAGE_STARTED),
    UPGRADE_IN_PROGRESS(ResourceEvent.SDX_UPGRADE_STARTED),
    UPGRADE_FAILED(ResourceEvent.SDX_UPGRADE_FAILED),
    DATALAKE_UPGRADE_IN_PROGRESS(ResourceEvent.DATALAKE_UPGRADE_STARTED),
    DATALAKE_UPGRADE_FAILED(ResourceEvent.DATALAKE_UPGRADE_FAILED),
    DELETE_REQUESTED(ResourceEvent.SDX_CLUSTER_DELETION_STARTED),
    DELETED(ResourceEvent.SDX_CLUSTER_DELETION_FINISHED),
    DELETE_FAILED(ResourceEvent.SDX_CLUSTER_DELETION_FAILED),
    START_IN_PROGRESS(ResourceEvent.SDX_START_STARTED),
    START_FAILED(ResourceEvent.SDX_START_FAILED),
    STOP_IN_PROGRESS(ResourceEvent.SDX_STOP_STARTED),
    STOP_FAILED(ResourceEvent.SDX_STOP_FAILED),
    STOPPED(ResourceEvent.SDX_STOP_FINISHED),
    CLUSTER_AMBIGUOUS(ResourceEvent.CLUSTER_AMBARI_CLUSTER_SYNCHRONIZED),
    SYNC_FAILED(ResourceEvent.SDX_SYNC_FAILED);

    private ResourceEvent resourceEvent;

    DatalakeStatusEnum(ResourceEvent resourceEvent) {
        this.resourceEvent = resourceEvent;
    }

    public boolean isDeleteInProgressOrCompleted() {
        return EXTERNAL_DATABASE_DELETION_IN_PROGRESS.equals(this)
                || STACK_DELETED.equals(this)
                || STACK_DELETION_IN_PROGRESS.equals(this)
                || DELETE_REQUESTED.equals(this)
                || DELETED.equals(this);
    }

    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    public DatalakeStatusEnum mapToFailedIfInProgress() {
        switch (this) {
            case START_IN_PROGRESS:
                return START_FAILED;
            case STOP_IN_PROGRESS:
                return STOP_FAILED;
            case REQUESTED:
            case WAIT_FOR_ENVIRONMENT:
            case STACK_CREATION_IN_PROGRESS:
            case ENVIRONMENT_CREATED:
            case EXTERNAL_DATABASE_CREATION_IN_PROGRESS:
                return PROVISIONING_FAILED;
            case STACK_DELETION_IN_PROGRESS:
            case EXTERNAL_DATABASE_DELETION_IN_PROGRESS:
                return DELETE_FAILED;
            case REPAIR_IN_PROGRESS:
                return REPAIR_FAILED;
            case CHANGE_IMAGE_IN_PROGRESS:
            case UPGRADE_IN_PROGRESS:
                return UPGRADE_FAILED;
            case DELETE_REQUESTED:
                return DELETE_FAILED;
            case DATALAKE_UPGRADE_IN_PROGRESS:
                return DATALAKE_UPGRADE_FAILED;
            default:
                return this;
        }
    }

    public ResourceEvent getDefaultResourceEvent() {
        return resourceEvent;
    }

}
