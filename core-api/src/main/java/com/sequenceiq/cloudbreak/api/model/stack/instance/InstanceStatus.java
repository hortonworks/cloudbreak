package com.sequenceiq.cloudbreak.api.model.stack.instance;

public enum InstanceStatus {
    REQUESTED,
    FAILED,
    CREATED,
    ORCHESTRATION_FAILED,
    SERVICES_RUNNING,
    SERVICES_HEALTHY,
    SERVICES_UNHEALTHY,
    WAITING_FOR_REPAIR,
    STOPPED,
    DELETED_ON_PROVIDER_SIDE,
    DELETE_REQUESTED,
    TERMINATED
}
