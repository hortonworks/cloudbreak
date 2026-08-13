package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base;

public enum InstanceStatus {
    REQUESTED,
    FAILED(true),
    CREATED,
    ORCHESTRATION_FAILED(true),
    SERVICES_RUNNING,
    SERVICES_HEALTHY,
    SERVICES_UNHEALTHY(true),
    WAITING_FOR_REPAIR(true),
    STOPPED,
    DELETING_FROM_PROVIDER_SIDE(true),
    DELETED_ON_PROVIDER_SIDE(true),
    DELETED_BY_PROVIDER(true),
    DELETE_REQUESTED,
    UNDER_DECOMMISSION,
    REMOVING_FROM_CLUSTER_MANAGER(true),
    DECOMMISSIONED,
    RESTARTING,
    DECOMMISSION_FAILED(true),
    TERMINATED,
    ZOMBIE(true);

    private final boolean instanceNotificationRequired;

    InstanceStatus() {
        this(false);
    }

    InstanceStatus(boolean instanceNotificationRequired) {
        this.instanceNotificationRequired = instanceNotificationRequired;
    }

    public boolean isInstanceNotificationRequired() {
        return instanceNotificationRequired;
    }

    public static boolean isActive(InstanceStatus status) {
        return status == SERVICES_RUNNING || status == SERVICES_HEALTHY;
    }

    public String getAsHostState() {
        switch (this) {
            case SERVICES_HEALTHY:
                return "HEALTHY";
            case DECOMMISSION_FAILED:
            case DELETED_ON_PROVIDER_SIDE:
            case DELETED_BY_PROVIDER:
            case SERVICES_UNHEALTHY:
                return "UNHEALTHY";
            case WAITING_FOR_REPAIR:
                return "WAITING_FOR_REPAIR";
            case SERVICES_RUNNING:
                return "RUNNING";
            default:
                return "UNKNOWN";
        }
    }
}
